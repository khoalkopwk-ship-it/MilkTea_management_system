package vn.edu.ute.milktea.entity.voucher;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.Order;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "LichSuSuDungVoucher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLichSu")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaVoucher", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaHD")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCN", nullable = false)
    private Branch branch;

    @Column(name = "SoTienGiam", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 20)
    @Builder.Default
    private VoucherUsageStatus status = VoucherUsageStatus.DA_DUNG;

    @Column(name = "NgaySuDung", nullable = false)
    @Builder.Default
    private Instant usedAt = Instant.now();

    @Column(name = "NgayHoanTra")
    private Instant refundedAt;

    @Column(name = "LyDoHoanTra", length = 500)
    private String refundReason;
}
