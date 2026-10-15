package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AuthDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.EmailVerificationOtp;
import vn.edu.ute.milktea.entity.account.PasswordResetToken;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.account.EmailVerificationOtpRepository;
import vn.edu.ute.milktea.repository.account.PasswordResetTokenRepository;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.security.JwtService;
import vn.edu.ute.milktea.service.auth.AuthService;
import vn.edu.ute.milktea.service.auth.OtpService;
import vn.edu.ute.milktea.service.mail.MailService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthEmailVerificationFlowTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationOtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private GuestAccessService guestAccessService;

    @Mock
    private MailService mailService;

    private OtpService otpService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpRepository);
        ReflectionTestUtils.setField(otpService, "otpSecret", "test_very_secret_otp_key_2026");
        ReflectionTestUtils.setField(otpService, "cooldownSeconds", 60);
        ReflectionTestUtils.setField(otpService, "defaultMaxAttempts", 5);

        authService = new AuthService(
                accountRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtService,
                guestAccessService,
                otpService,
                mailService
        );

        when(passwordEncoder.encode(any())).thenAnswer(inv -> "hashed_" + inv.getArgument(0));
        when(passwordEncoder.matches(any(), any())).thenAnswer(inv ->
                ("hashed_" + inv.getArgument(0)).equals(inv.getArgument(1)));
    }

    @Test
    @DisplayName("1. Đăng ký mới: Tạo tài khoản CUSTOMER chưa xác thực, gửi OTP và KHÔNG cấp JWT")
    void testRegisterNewCustomer_Success_PendingVerification() {
        String email = "testuser@gmail.com";
        AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
                .fullName("Nguyen Van Test")
                .email(email)
                .phone("0901234567")
                .password("Password@123")
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account acc = inv.getArgument(0);
            acc.setId(100L);
            return acc;
        });

        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthDto.RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(email, response.getEmail());
        assertFalse(response.isEmailVerified(), "Tài khoản mới bắt buộc chưa xác thực email");
        assertEquals(60, response.getCooldownSeconds());

        // Kiểm tra tài khoản được lưu với CUSTOMER và emailVerified = false
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertEquals(Role.CUSTOMER, savedAccount.getRole(), "Đăng ký công khai bắt buộc là CUSTOMER");
        assertFalse(savedAccount.getEmailVerified(), "emailVerified phải là false");
        assertTrue(savedAccount.getActive(), "Tài khoản phải active");

        // Kiểm tra đã gọi gửi mail SMTP thật
        verify(mailService, times(1)).sendRegistrationOtpEmail(eq(email), eq("Nguyen Van Test"), anyString(), eq(10));
        // Kiểm tra KHÔNG gọi sinh JWT
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("2. Đăng nhập: Bị từ chối nếu email chưa xác thực (AUTH_EMAIL_NOT_VERIFIED)")
    void testLogin_Fails_WhenEmailNotVerified() {
        String email = "unverified@gmail.com";
        Account unverifiedAccount = Account.builder()
                .id(200L)
                .email(email)
                .passwordHash("hashed_Password@123")
                .fullName("Unverified User")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(false) // Chưa xác thực
                .tokenVersion(1L)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(unverifiedAccount));

        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email(email)
                .password("Password@123")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        assertEquals(ErrorCode.AUTH_EMAIL_NOT_VERIFIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("chưa được xác thực"));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("3. Đăng nhập: Tách biệt lỗi tài khoản bị khóa (ACCESS_DENIED) khỏi lỗi chưa xác thực")
    void testLogin_Fails_WhenAccountLocked() {
        String email = "locked@gmail.com";
        Account lockedAccount = Account.builder()
                .id(300L)
                .email(email)
                .passwordHash("hashed_Password@123")
                .role(Role.CUSTOMER)
                .active(false) // Bị khóa bởi admin
                .emailVerified(true)
                .tokenVersion(1L)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(lockedAccount));

        AuthDto.LoginRequest loginRequest = AuthDto.LoginRequest.builder()
                .email(email)
                .password("Password@123")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("tạm khóa"));
    }

    @Test
    @DisplayName("4. Xác thực OTP thành công: Kích hoạt email và cho phép đăng nhập nhận JWT")
    void testVerifyOtp_Success_EnablesLogin() {
        String email = "activeuser@gmail.com";
        Account account = Account.builder()
                .id(400L)
                .email(email)
                .passwordHash("hashed_Password@123")
                .fullName("Active User")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(false)
                .tokenVersion(1L)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // Sinh OTP thật thông qua OtpService
        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(email, EmailVerificationOtp.PURPOSE_REGISTER, 10);
        String rawOtp = generated.rawOtp();
        EmailVerificationOtp otpEntity = generated.entity();

        // Giả lập tìm lại được OTP vừa sinh
        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(eq(email), eq(EmailVerificationOtp.PURPOSE_REGISTER)))
                .thenReturn(Optional.of(otpEntity));

        // Thực hiện xác thực email
        AuthDto.VerifyEmailRequest verifyReq = new AuthDto.VerifyEmailRequest(email, rawOtp);
        String verifyMsg = authService.verifyEmail(verifyReq);
        assertNotNull(verifyMsg);
        assertTrue(account.getEmailVerified(), "Tài khoản phải chuyển thành emailVerified = true");
        assertNotNull(otpEntity.getUsedAt(), "OTP phải được đánh dấu đã dùng");

        // Sau khi đã xác thực -> đăng nhập thành công
        when(jwtService.generateToken(account)).thenReturn("jwt_token_sample");
        AuthDto.AuthResponse loginRes = authService.login(new AuthDto.LoginRequest(email, "Password@123"));
        assertNotNull(loginRes);
        assertEquals("jwt_token_sample", loginRes.getToken());
    }

    @Test
    @DisplayName("5. Nhập sai OTP: Đếm số lần thử và khóa sau 5 lần sai")
    void testVerifyOtp_WrongCode_IncrementsAttempts_AndRevokesAfterMaxAttempts() {
        String email = "wrongcode@gmail.com";
        Account account = Account.builder().email(email).emailVerified(false).build();
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(otpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(email, EmailVerificationOtp.PURPOSE_REGISTER, 10);
        EmailVerificationOtp otpEntity = generated.entity();

        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(eq(email), eq(EmailVerificationOtp.PURPOSE_REGISTER)))
                .thenReturn(Optional.of(otpEntity));

        // Lần 1: Nhập sai mã
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                authService.verifyEmail(new AuthDto.VerifyEmailRequest(email, "000000")));
        assertEquals(ErrorCode.AUTH_OTP_INVALID, ex1.getErrorCode());
        assertEquals(1, otpEntity.getAttempts());
        assertTrue(ex1.getMessage().contains("Bạn còn 4 lần thử"));

        // Lần 2, 3, 4 nhập sai tiếp
        otpEntity.setAttempts(4);
        BusinessException ex5 = assertThrows(BusinessException.class, () ->
                authService.verifyEmail(new AuthDto.VerifyEmailRequest(email, "000000")));
        assertEquals(ErrorCode.AUTH_OTP_INVALID, ex5.getErrorCode());
        assertEquals(5, otpEntity.getAttempts());
        assertNotNull(otpEntity.getRevokedAt(), "Mã phải bị thu hồi sau 5 lần thử sai");
    }

    @Test
    @DisplayName("6. Giới hạn tần suất: Gửi lại mã trước 60s bị từ chối (AUTH_RATE_LIMITED)")
    void testResendOtp_RateLimitCooldown() {
        String email = "cooldown@gmail.com";
        Account account = Account.builder().email(email).emailVerified(false).build();
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));

        EmailVerificationOtp recentOtp = EmailVerificationOtp.builder()
                .email(email)
                .purpose(EmailVerificationOtp.PURPOSE_REGISTER)
                .lastSentAt(Instant.now().minus(20, ChronoUnit.SECONDS)) // Mới gửi 20s trước
                .createdAt(Instant.now().minus(20, ChronoUnit.SECONDS))
                .build();

        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(eq(email), eq(EmailVerificationOtp.PURPOSE_REGISTER)))
                .thenReturn(Optional.of(recentOtp));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.resendRegisterOtp(email));
        assertEquals(ErrorCode.AUTH_RATE_LIMITED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Vui lòng đợi"));
    }

    @Test
    @DisplayName("7. Quên mật khẩu: Gửi mã khôi phục, đổi thành công tăng tokenVersion và vô hiệu session cũ")
    void testPasswordReset_Success_IncrementsTokenVersion() {
        String email = "customer@milktea.vn";
        Account account = Account.builder()
                .id(500L)
                .email(email)
                .passwordHash("hashed_OldPassword@123")
                .fullName("Customer Old")
                .active(true)
                .tokenVersion(1L)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Yêu cầu đặt lại mật khẩu
        String msg = authService.requestPasswordReset(new AuthDto.PasswordResetRequest(email));
        assertNotNull(msg);
        assertFalse(msg.contains("123"), "Không được phát lộ mã trong response công khai");
        verify(mailService, times(1)).sendPasswordResetOtpEmail(eq(email), any(), anyString(), eq(15));

        // 2. Lấy mã vừa sinh ra và giả lập verify
        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(email, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, 15);
        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(eq(email), eq(EmailVerificationOtp.PURPOSE_PASSWORD_RESET)))
                .thenReturn(Optional.of(generated.entity()));

        // 3. Đổi mật khẩu
        AuthDto.PasswordResetConfirmRequest confirmReq = new AuthDto.PasswordResetConfirmRequest(
                email, generated.rawOtp(), null, "NewPassword@456");

        authService.confirmPasswordReset(confirmReq);

        assertEquals("hashed_NewPassword@456", account.getPasswordHash());
        assertEquals(2L, account.getTokenVersion(), "TokenVersion phải tăng để vô hiệu hóa JWT cũ");
    }

    @Test
    @DisplayName("8. Bảo vệ mục đích mã: Mã REGISTER_VERIFY không được dùng để PASSWORD_RESET")
    void testCrossPurposeOtpProtection() {
        String email = "crosspurpose@gmail.com";
        when(otpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        // Sinh mã REGISTER_VERIFY
        OtpService.GeneratedOtp regOtp = otpService.generateAndSaveOtp(email, EmailVerificationOtp.PURPOSE_REGISTER, 10);

        // Giả lập tìm kiếm theo mục đích PASSWORD_RESET sẽ không thấy mã đăng ký
        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(eq(email), eq(EmailVerificationOtp.PURPOSE_PASSWORD_RESET)))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                otpService.verifyOtp(email, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, regOtp.rawOtp()));
    }

    @Test
    @DisplayName("9. Quy trình 3 bước: Phải xác thực OTP đúng trước mới cấp resetToken để đổi mật khẩu")
    void testVerifyPasswordResetOtpBeforePasswordChange() {
        String email = "customer3step@milktea.vn";
        Account account = Account.builder()
                .id(600L)
                .email(email)
                .passwordHash("hashed_OldPassword@123")
                .fullName("Customer 3-Step")
                .active(true)
                .tokenVersion(1L)
                .build();

        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        when(otpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestAccessService.hashToken(anyString())).thenAnswer(inv -> "hash_" + inv.getArgument(0));

        // 1. Sinh mã OTP
        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(email, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, 15);
        when(otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(
                eq(email), eq(EmailVerificationOtp.PURPOSE_PASSWORD_RESET)))
                .thenReturn(Optional.of(generated.entity()));

        // Thử nhập sai OTP -> ném ngoại lệ
        assertThrows(BusinessException.class, () ->
                authService.verifyPasswordResetOtp(email, "999999"));

        // Xác thực đúng OTP -> trả về resetToken tạm thời
        String resetToken = authService.verifyPasswordResetOtp(email, generated.rawOtp());
        assertNotNull(resetToken);
        assertFalse(resetToken.isBlank());

        // 2. Dùng resetToken để đặt mật khẩu mới
        PasswordResetToken savedTokenEntity = PasswordResetToken.builder()
                .account(account)
                .tokenHash("hash_" + resetToken)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();
        when(passwordResetTokenRepository.findByTokenHash("hash_" + resetToken))
                .thenReturn(Optional.of(savedTokenEntity));

        AuthDto.PasswordResetConfirmRequest confirmReq = new AuthDto.PasswordResetConfirmRequest(
                email, null, resetToken, "BrandNewPassword@789");

        authService.confirmPasswordReset(confirmReq);

        assertEquals("hashed_BrandNewPassword@789", account.getPasswordHash());
        assertEquals(2L, account.getTokenVersion());
        assertNotNull(savedTokenEntity.getUsedAt(), "ResetToken phải được đánh dấu đã sử dụng");

        // 3. Không thể tái sử dụng token đã dùng
        assertThrows(BusinessException.class, () -> authService.confirmPasswordReset(confirmReq));
    }
}
