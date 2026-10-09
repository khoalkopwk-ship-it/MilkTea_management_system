package vn.edu.ute.milktea.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AuthDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.PasswordResetToken;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.account.PasswordResetTokenRepository;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.security.JwtService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GuestAccessService guestAccessService;

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> BusinessException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mật khẩu không đúng"));

        if (!Boolean.TRUE.equals(account.getActive())) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Tài khoản đã bị tạm khóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "Tài khoản hoặc mật khẩu không đúng");
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

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (accountRepository.existsByEmail(email)) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Email đã được sử dụng");
        }

        Account account = Account.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.CUSTOMER) // Khách đăng ký chỉ được role CUSTOMER
                .active(true)
                .tokenVersion(1L)
                .createdAt(Instant.now())
                .build();

        accountRepository.save(account);

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

    @Transactional
    public String requestPasswordReset(AuthDto.PasswordResetRequest request) {
        var accountOpt = accountRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (accountOpt.isEmpty()) {
            return "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được xử lý.";
        }

        Account account = accountOpt.get();
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = guestAccessService.hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .account(account)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Trong môi trường thực tế sẽ gửi mail; trả thông điệp an toàn
        return rawToken;
    }

    @Transactional
    public void confirmPasswordReset(AuthDto.PasswordResetConfirmRequest request) {
        String tokenHash = guestAccessService.hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> BusinessException.badRequest(ErrorCode.AUTH_INVALID_CREDENTIALS, "Mã khôi phục không hợp lệ hoặc đã hết hạn"));

        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw BusinessException.badRequest(ErrorCode.AUTH_TOKEN_EXPIRED, "Mã khôi phục đã hết hạn hoặc đã sử dụng");
        }

        Account account = resetToken.getAccount();
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setTokenVersion(account.getTokenVersion() + 1); // Vô hiệu token cũ
        accountRepository.save(account);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
    }
}
