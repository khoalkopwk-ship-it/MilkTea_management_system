package vn.edu.ute.milktea.entity.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TruyCapDonQuay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounterOrderAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTruyCap")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private Order order;

    @Column(name = "TokenHash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "HetHanLuc", nullable = false)
    private Instant expiresAt;

    @Column(name = "ThuHoiLuc")
    private Instant revokedAt;
}
