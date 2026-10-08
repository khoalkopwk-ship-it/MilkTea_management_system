package vn.edu.ute.milktea.entity.account;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TokenKhoiPhuc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaToken")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK", nullable = false)
    private Account account;

    @Column(name = "TokenHash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "HetHanLuc", nullable = false)
    private Instant expiresAt;

    @Column(name = "DaDungLuc")
    private Instant usedAt;
}
