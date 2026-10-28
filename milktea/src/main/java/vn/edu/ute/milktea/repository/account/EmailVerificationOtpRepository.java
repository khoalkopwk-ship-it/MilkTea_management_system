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

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmailVerificationOtp o SET o.attempts = o.attempts + 1, o.revokedAt = CASE WHEN (o.attempts + 1) >= o.maxAttempts THEN :now ELSE o.revokedAt END WHERE o.id = :id AND o.usedAt IS NULL AND o.revokedAt IS NULL")
    int incrementAttemptsAtomically(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("now") java.time.Instant now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmailVerificationOtp o SET o.usedAt = :now WHERE o.id = :id AND o.usedAt IS NULL AND o.revokedAt IS NULL AND o.expiresAt > :now AND o.attempts < o.maxAttempts")
    int consumeOtpAtomically(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
