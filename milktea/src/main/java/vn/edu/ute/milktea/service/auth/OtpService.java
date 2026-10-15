package vn.edu.ute.milktea.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.entity.account.EmailVerificationOtp;
import vn.edu.ute.milktea.repository.account.EmailVerificationOtpRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailVerificationOtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.secret:milktea_secure_otp_secret_key_2026_very_long_and_safe}")
    private String otpSecret;

    @Value("${app.otp.cooldown-seconds:60}")
    private int cooldownSeconds;

    @Value("${app.otp.max-attempts:5}")
    private int defaultMaxAttempts;

    public record GeneratedOtp(String rawOtp, EmailVerificationOtp entity) {}

    /**
     * Tạo mã OTP mới, kiểm tra cooldown và vô hiệu mã cũ chưa dùng.
     */
    @Transactional
    public GeneratedOtp generateAndSaveOtp(String email, String purpose, int ttlMinutes) {
        String cleanEmail = email.trim().toLowerCase();
        Instant now = Instant.now();

        // 1. Kiểm tra cooldown chống gửi dồn dập / spam
        var latestActiveOpt = otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(
                cleanEmail, purpose);

        if (latestActiveOpt.isPresent()) {
            EmailVerificationOtp latest = latestActiveOpt.get();
            long elapsedSeconds = Duration.between(latest.getLastSentAt(), now).toSeconds();
            if (elapsedSeconds < cooldownSeconds) {
                long waitSeconds = cooldownSeconds - elapsedSeconds;
                throw BusinessException.badRequest(
                        ErrorCode.AUTH_RATE_LIMITED,
                        "Vui lòng đợi " + waitSeconds + " giây trước khi yêu cầu gửi lại mã xác thực mới."
                );
            }
        }

        // 2. Vô hiệu hóa tất cả mã cũ chưa dùng của email và purpose này
        List<EmailVerificationOtp> oldOtps = otpRepository.findAllByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNull(
                cleanEmail, purpose);
        for (EmailVerificationOtp old : oldOtps) {
            old.setRevokedAt(now);
        }
        if (!oldOtps.isEmpty()) {
            otpRepository.saveAll(oldOtps);
        }

        // 3. Sinh OTP 6 chữ số ngẫu nhiên an toàn bằng SecureRandom
        int number = secureRandom.nextInt(1_000_000);
        String rawOtp = String.format("%06d", number);

        // 4. Sinh salt ngẫu nhiên 16 bytes cho từng bản ghi
        byte[] saltBytes = new byte[16];
        secureRandom.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);

        // 5. Băm OTP bằng HMAC-SHA256 có secret và salt gắn liền email + purpose
        String otpHash = computeHmacHash(rawOtp, salt, cleanEmail, purpose);

        EmailVerificationOtp otpEntity = EmailVerificationOtp.builder()
                .email(cleanEmail)
                .otpHash(otpHash)
                .salt(salt)
                .purpose(purpose)
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .attempts(0)
                .maxAttempts(defaultMaxAttempts)
                .lastSentAt(now)
                .createdAt(now)
                .build();

        EmailVerificationOtp saved = otpRepository.save(otpEntity);
        return new GeneratedOtp(rawOtp, saved);
    }

    /**
     * Xác thực mã OTP và cập nhật trạng thái used/attempts.
     */
    @Transactional
    public void verifyOtp(String email, String purpose, String rawOtp) {
        String cleanEmail = email.trim().toLowerCase();
        String cleanOtp = rawOtp != null ? rawOtp.trim() : "";

        if (cleanOtp.length() != 6 || !cleanOtp.matches("^\\d{6}$")) {
            throw BusinessException.badRequest(ErrorCode.AUTH_OTP_INVALID, "Mã xác thực phải gồm đúng 6 chữ số.");
        }

        var otpOpt = otpRepository.findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(
                cleanEmail, purpose);

        if (otpOpt.isEmpty()) {
            throw BusinessException.badRequest(
                    ErrorCode.AUTH_OTP_INVALID,
                    "Không tìm thấy mã xác thực hợp lệ hoặc mã đã bị hủy. Vui lòng yêu cầu mã mới."
            );
        }

        EmailVerificationOtp otp = otpOpt.get();
        Instant now = Instant.now();

        // Kiểm tra hết hạn
        if (otp.getExpiresAt().isBefore(now)) {
            otp.setRevokedAt(now);
            otpRepository.save(otp);
            throw BusinessException.badRequest(
                    ErrorCode.AUTH_TOKEN_EXPIRED,
                    "Mã xác thực đã hết hạn. Vui lòng nhấn 'Gửi lại mã' để nhận mã mới."
            );
        }

        // Kiểm tra số lần thử tối đa
        if (otp.getAttempts() >= otp.getMaxAttempts()) {
            otp.setRevokedAt(now);
            otpRepository.save(otp);
            throw BusinessException.badRequest(
                    ErrorCode.AUTH_OTP_INVALID,
                    "Mã xác thực đã bị khóa do thử sai quá " + otp.getMaxAttempts() + " lần. Vui lòng yêu cầu gửi lại mã mới."
            );
        }

        // So sánh HMAC Hash
        String expectedHash = computeHmacHash(cleanOtp, otp.getSalt(), cleanEmail, purpose);
        if (!expectedHash.equals(otp.getOtpHash())) {
            int newAttempts = otp.getAttempts() + 1;
            otp.setAttempts(newAttempts);
            if (newAttempts >= otp.getMaxAttempts()) {
                otp.setRevokedAt(now);
            }
            otpRepository.save(otp);

            int remaining = Math.max(0, otp.getMaxAttempts() - newAttempts);
            if (remaining > 0) {
                throw BusinessException.badRequest(
                        ErrorCode.AUTH_OTP_INVALID,
                        "Mã xác thực không chính xác. Bạn còn " + remaining + " lần thử."
                );
            } else {
                throw BusinessException.badRequest(
                        ErrorCode.AUTH_OTP_INVALID,
                        "Mã xác thực đã bị khóa do nhập sai quá " + otp.getMaxAttempts() + " lần. Vui lòng yêu cầu mã mới."
                );
            }
        }

        // Khớp mã -> đánh dấu đã dùng
        otp.setUsedAt(now);
        otpRepository.save(otp);
    }

    /**
     * Băm HMAC-SHA256 bảo vệ không gian OTP nhỏ.
     */
    private String computeHmacHash(String rawOtp, String salt, String email, String purpose) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(otpSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            String payload = salt + ":" + rawOtp + ":" + email + ":" + purpose;
            byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            log.error("Lỗi tính toán HMAC cho OTP: {}", e.getMessage());
            throw new RuntimeException("Lỗi bảo mật khi băm mã OTP", e);
        }
    }
}
