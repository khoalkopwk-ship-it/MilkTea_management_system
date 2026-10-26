package vn.edu.ute.milktea.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.entity.account.EmailVerificationOtp;
import vn.edu.ute.milktea.repository.account.EmailVerificationOtpRepository;

import java.time.Instant;

/**
 * Ghi nhận số lần thử sai OTP trong Transaction độc lập (REQUIRES_NEW).
 * Đảm bảo số lần thử sai luôn được commit vào CSDL kể cả khi nghiệp vụ
 * xác thực ném ngoại lệ BusinessException và rollback outer transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpAttemptRecorder {

    private final EmailVerificationOtpRepository otpRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttempt(Long otpId, int maxAttempts) {
        if (otpId == null) return 0;

        EmailVerificationOtp otp = otpRepository.findById(otpId).orElse(null);
        if (otp == null) return 0;

        int newAttempts = otp.getAttempts() + 1;
        otp.setAttempts(newAttempts);
        if (newAttempts >= maxAttempts) {
            otp.setRevokedAt(Instant.now());
            log.warn("Mã OTP id={} đã bị thu hồi do nhập sai quá {} lần", otpId, maxAttempts);
        }
        otpRepository.saveAndFlush(otp);
        return newAttempts;
    }
}
