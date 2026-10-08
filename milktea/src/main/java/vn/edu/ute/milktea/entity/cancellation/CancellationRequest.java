package vn.edu.ute.milktea.entity.cancellation;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.order.Order;

import java.time.Instant;

@Entity
@Table(name = "YeuCauHuy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaHuy")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private Order order;

    @Column(name = "LyDo", nullable = false, length = 500)
    private String reason;

    @Column(name = "LyDoXuLy", length = 500)
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 16)
    @Builder.Default
    private CancellationStatus status = CancellationStatus.CHO;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "XuLyLuc")
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiXuLy")
    private Account decidedBy;
}
