package vn.edu.ute.milktea.entity.cancellation;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.payment.Payment;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "YeuCauHoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaHoan")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false, unique = true)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTT", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "SoTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "LyDo", nullable = false, length = 500)
    private String reason;

    @Column(name = "LyDoDuyet", length = 500)
    private String approvalReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 16)
    @Builder.Default
    private RefundStatus status = RefundStatus.CHO_DUYET;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiDuyet")
    private Account approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiHoan")
    private Account refundedBy;

    @Column(name = "DuyetLuc")
    private Instant approvedAt;

    @Column(name = "HoanLuc")
    private Instant refundedAt;

    @Column(name = "ThamChieuHoan", length = 120)
    private String refundReference;

    @Column(name = "MaYeuCau", nullable = false, unique = true, length = 64)
    private String idempotencyKey;
}
