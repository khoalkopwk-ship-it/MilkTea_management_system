package vn.edu.ute.milktea.entity.table;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TruyCapPhienBan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSessionAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTruyCap")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhien", nullable = false)
    private TableSession session;

    @Column(name = "TokenHash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "HetHanLuc", nullable = false)
    private Instant expiresAt;

    @Column(name = "ThuHoiLuc")
    private Instant revokedAt;
}
