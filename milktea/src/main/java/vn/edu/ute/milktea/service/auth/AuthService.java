package vn.edu.ute.milktea.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AuthDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.EmailVerificationOtp;
import vn.edu.ute.milktea.entity.account.PasswordResetToken;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.account.PasswordResetTokenRepository;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.security.JwtService;
import vn.edu.ute.milktea.service.mail.MailService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GuestAccessService guestAccessService;
    private final OtpService otpService;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.unauthorized(
                        ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mật khẩu không đúng"));

        // 1. Kiểm tra tài khoản bị admin khóa
        if (!Boolean.TRUE.equals(account.getActive())) {
            throw BusinessException.forbidden(
                    ErrorCode.ACCESS_DENIED, "Tài khoản đã bị tạm khóa. Vui lòng liên hệ ban quản trị.");
        }

        // 2. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw BusinessException.unauthorized(
                    ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mật khẩu không đúng");
        }

        // 3. Kiểm tra xác thực email (Tách biệt khỏi trạng thái khóa)
        if (!Boolean.TRUE.equals(account.getEmailVerified())) {
            throw BusinessException.unauthorized(
                    ErrorCode.AUTH_EMAIL_NOT_VERIFIED,
                    "Địa chỉ email chưa được xác thực. Vui lòng xác thực email để kích hoạt tài khoản trước khi đăng nhập.");
        }

        String token = jwtService.generateToken(account);
        return AuthDto.AuthResponse.builder()
                .token(token)
                .user(AuthDto.UserInfo.builder()
                        .id(account.getId())
                        .email(account.getEmail())
                        .fullName(account.getFullName())
                        .phone(account.getPhone())
                        .role(account.getRole())
                        .build())
                .build();
    }

    /**
     * Đăng ký tài khoản công khai:
     * - Chỉ gán role CUSTOMER (bỏ qua role client gửi).
     * - Lưu tài khoản ở trạng thái emailVerified = false.
     * - Sinh mã OTP 6 số và gửi qua email.
     * - KHÔNG cấp JWT token khi chưa xác thực email.
     */
    @Transactional
    public AuthDto.RegisterResponse register(AuthDto.RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<Account> existingOpt = accountRepository.findByEmail(email);

        Account account;
        if (existingOpt.isPresent()) {
            Account existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Địa chỉ email này đã được sử dụng");
            }
            // Nếu tài khoản cũ chưa xác thực email, cập nhật thông tin mới và gửi lại OTP
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                existing.setFullName(request.getFullName().trim());
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                existing.setPhone(request.getPhone().trim());
            }
            account = accountRepository.save(existing);
        } else {
            account = Account.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                    .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                    .role(Role.CUSTOMER) // Khách đăng ký công khai bắt buộc là CUSTOMER
                    .active(true)
                    .emailVerified(false) // Chưa xác thực email
                    .tokenVersion(1L)
                    .createdAt(Instant.now())
                    .build();
            account = accountRepository.save(account);
        }

        // Sinh mã OTP 6 số bảo vệ HMAC-SHA256, hạn 10 phút
        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(
                email, EmailVerificationOtp.PURPOSE_REGISTER, 10);

        // Gửi email thật qua SMTP
        mailService.sendRegistrationOtpEmail(email, account.getFullName(), generated.rawOtp(), 10);

        return AuthDto.RegisterResponse.builder()
                .email(email)
                .maskedEmail(MailService.maskEmail(email))
                .message("Đăng ký thành công! Mã xác thực 6 chữ số đã được gửi tới email của bạn. Vui lòng nhập mã để kích hoạt tài khoản.")
                .emailVerified(false)
                .cooldownSeconds(60)
                .build();
    }

    /**
     * Xác thực email bằng mã OTP 6 chữ số.
     */
    @Transactional
    public String verifyEmail(AuthDto.VerifyEmailRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản với email này"));

        if (Boolean.TRUE.equals(account.getEmailVerified())) {
            return "Tài khoản của bạn đã được xác thực trước đó. Bạn có thể đăng nhập ngay.";
        }

        // Xác thực mã OTP (kiểm tra hạn dùng, băm HMAC, số lần thử)
        otpService.verifyOtp(email, EmailVerificationOtp.PURPOSE_REGISTER, request.getOtp());

        // Cập nhật trạng thái xác thực email
        // LƯU Ý: Không tự mở khóa nếu tài khoản đang bị admin khóa (active = false)
        account.setEmailVerified(true);
        accountRepository.save(account);

        return "Xác thực email thành công! Bạn có thể đăng nhập vào hệ thống ngay bây giờ.";
    }

    /**
     * Gửi lại mã OTP xác thực email đăng ký (có cooldown tối thiểu 60s).
     */
    @Transactional
    public String resendRegisterOtp(String email) {
        String cleanEmail = email.trim().toLowerCase();
        Account account = accountRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Địa chỉ email chưa được đăng ký trong hệ thống"));

        if (Boolean.TRUE.equals(account.getEmailVerified())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Tài khoản đã được xác thực. Bạn có thể đăng nhập ngay.");
        }

        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(
                cleanEmail, EmailVerificationOtp.PURPOSE_REGISTER, 10);

        mailService.sendRegistrationOtpEmail(cleanEmail, account.getFullName(), generated.rawOtp(), 10);

        return "Mã xác thực mới đã được gửi tới email " + MailService.maskEmail(cleanEmail) + ". Vui lòng kiểm tra hộp thư.";
    }

    @Transactional
    public void logout(Long accountId) {
        if (accountId != null) {
            accountRepository.findById(accountId).ifPresent(account -> {
                // Tăng tokenVersion để vô hiệu hóa mọi token cũ
                account.setTokenVersion(account.getTokenVersion() + 1);
                accountRepository.save(account);
            });
        }
    }

    /**
     * Yêu cầu đặt lại mật khẩu:
     * - Response công khai luôn trả cùng một thông báo chung (không tiết lộ email tồn tại hay không).
     * - Nếu tồn tại và active: sinh mã OTP khôi phục 6 số và gửi qua email.
     * - Tuyệt đối không trả mã thô trong response.
     */
    @Transactional
    public String requestPasswordReset(AuthDto.PasswordResetRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String genericMessage = "Nếu địa chỉ email tồn tại trên hệ thống, mã xác thực khôi phục mật khẩu đã được gửi đến hộp thư của bạn.";

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            log.info("Yêu cầu reset mật khẩu cho email không tồn tại: {}", MailService.maskEmail(email));
            return genericMessage;
        }

        Account account = accountOpt.get();
        if (!Boolean.TRUE.equals(account.getActive())) {
            log.warn("Yêu cầu reset mật khẩu cho tài khoản đang bị khóa: {}", MailService.maskEmail(email));
            return genericMessage;
        }

        // Sinh mã OTP 6 số mục đích PASSWORD_RESET, thời hạn 15 phút
        OtpService.GeneratedOtp generated = otpService.generateAndSaveOtp(
                email, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, 15);

        // Gửi email thật
        mailService.sendPasswordResetOtpEmail(email, account.getFullName(), generated.rawOtp(), 15);

        return genericMessage;
    }

    /**
     * Xác thực mã OTP đặt lại mật khẩu và cấp vé tạm thời (resetToken) có hiệu lực 15 phút.
     */
    @Transactional
    public String verifyPasswordResetOtp(String rawEmail, String otp) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Email không được để trống");
        }
        String cleanEmail = rawEmail.trim().toLowerCase();
        Account account = accountRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> BusinessException.badRequest(
                        ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mã xác thực không hợp lệ"));

        if (!Boolean.TRUE.equals(account.getActive())) {
            throw BusinessException.badRequest(ErrorCode.ACCESS_DENIED, "Tài khoản đang bị tạm khóa.");
        }

        // 1. Xác thực mã OTP 6 số với purpose PASSWORD_RESET
        otpService.verifyOtp(cleanEmail, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, otp);

        // 2. Sinh resetToken ngẫu nhiên an toàn, lưu hash vào TokenKhoiPhuc
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = guestAccessService.hashToken(rawToken);

        PasswordResetToken resetTokenEntity = PasswordResetToken.builder()
                .account(account)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        passwordResetTokenRepository.save(resetTokenEntity);
        log.info("Xác thực OTP đặt lại mật khẩu thành công cho tài khoản: {}", MailService.maskEmail(cleanEmail));

        return rawToken;
    }

    /**
     * Xác nhận đặt lại mật khẩu bằng mã OTP 6 số hoặc token khôi phục hợp lệ.
     */
    @Transactional
    public void confirmPasswordReset(AuthDto.PasswordResetConfirmRequest request) {
        String tokenOrCode = request.getEffectiveToken();
        if (tokenOrCode.isBlank()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Mã xác thực không được để trống");
        }

        Account account = null;

        // Trường hợp 1: Sử dụng mã OTP 6 chữ số kèm email
        if (request.getEmail() != null && !request.getEmail().isBlank() && tokenOrCode.matches("^\\d{6}$")) {
            String email = request.getEmail().trim().toLowerCase();
            account = accountRepository.findByEmail(email)
                    .orElseThrow(() -> BusinessException.badRequest(
                            ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mã xác thực không hợp lệ"));

            otpService.verifyOtp(email, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, tokenOrCode);
        }
        // Trường hợp 2: Tra cứu token UUID trong TokenKhoiPhuc (nếu có dùng link)
        else {
            String tokenHash = guestAccessService.hashToken(tokenOrCode);
            Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByTokenHash(tokenHash);

            if (resetTokenOpt.isPresent()) {
                PasswordResetToken resetToken = resetTokenOpt.get();
                if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(Instant.now())) {
                    throw BusinessException.badRequest(ErrorCode.AUTH_TOKEN_EXPIRED, "Mã khôi phục đã hết hạn hoặc đã sử dụng");
                }
                account = resetToken.getAccount();
                resetToken.setUsedAt(Instant.now());
                passwordResetTokenRepository.save(resetToken);
            } else if (request.getEmail() != null && !request.getEmail().isBlank()) {
                // Thử xác thực qua OTP service nếu mã gửi dạng OTP
                String email = request.getEmail().trim().toLowerCase();
                account = accountRepository.findByEmail(email)
                        .orElseThrow(() -> BusinessException.badRequest(
                                ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mã xác thực không hợp lệ"));
                otpService.verifyOtp(email, EmailVerificationOtp.PURPOSE_PASSWORD_RESET, tokenOrCode);
            } else {
                throw BusinessException.badRequest(ErrorCode.AUTH_INVALID_CREDENTIALS, "Mã khôi phục không hợp lệ hoặc đã hết hạn");
            }
        }

        if (account == null) {
            throw BusinessException.badRequest(ErrorCode.AUTH_INVALID_CREDENTIALS, "Không thể xác định tài khoản cần đặt lại mật khẩu");
        }

        // Cập nhật mật khẩu băm mới
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // Tăng tokenVersion để vô hiệu hóa toàn bộ session/JWT cũ
        account.setTokenVersion(account.getTokenVersion() + 1);

        // Lưu tài khoản (Lưu ý: Không tự ý mở khóa active nếu tài khoản bị khóa)
        accountRepository.save(account);
        log.info("Đặt lại mật khẩu thành công cho tài khoản: {}", MailService.maskEmail(account.getEmail()));
    }

    @Transactional(readOnly = true)
    public AuthDto.UserInfo getProfile(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));
        return AuthDto.UserInfo.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .role(account.getRole())
                .build();
    }

    @Transactional
    public AuthDto.UserInfo updateProfile(Long accountId, AuthDto.UpdateProfileRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        if (request.getFullName() != null) {
            account.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone().trim());
        }
        accountRepository.save(account);

        return AuthDto.UserInfo.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .role(account.getRole())
                .build();
    }

    @Transactional
    public void changePassword(Long accountId, AuthDto.ChangePasswordRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw BusinessException.badRequest(ErrorCode.AUTH_INVALID_CREDENTIALS, "Mật khẩu hiện tại không chính xác");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setTokenVersion(account.getTokenVersion() + 1);
        accountRepository.save(account);
    }
}
