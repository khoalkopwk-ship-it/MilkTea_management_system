package vn.edu.ute.milktea.entity.account;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "XacThucEmail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationOtp {

    public static final String PURPOSE_REGISTER = "REGISTER_VERIFY";
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaOTP")
    private Long id;

    @Column(name = "Email", nullable = false, length = 254)
    private String email;

    @Column(name = "OtpHash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "Salt", nullable = false, length = 64)
    private String salt;

    @Column(name = "MucDich", nullable = false, length = 32)
    private String purpose;

    @Column(name = "HetHanLuc", nullable = false)
    private Instant expiresAt;

    @Column(name = "SoLanThu", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "ToiDaThu", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    @Column(name = "DaDungLuc")
    private Instant usedAt;

    @Column(name = "VoHieuLuc")
    private Instant revokedAt;

    @Column(name = "GuiCuoiLuc", nullable = false)
    private Instant lastSentAt;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastSentAt == null) {
            lastSentAt = now;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (maxAttempts == null) {
            maxAttempts = 5;
        }
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isMaxAttemptsReached() {
        return attempts != null && maxAttempts != null && attempts >= maxAttempts;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed() && !isRevoked() && !isMaxAttemptsReached();
    }
}
