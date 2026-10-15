package vn.edu.ute.milktea.repository.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.account.EmailVerificationOtp;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findTopByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNullOrderByCreatedAtDesc(
            String email, String purpose);

    List<EmailVerificationOtp> findAllByEmailAndPurposeAndRevokedAtIsNullAndUsedAtIsNull(
            String email, String purpose);
}
