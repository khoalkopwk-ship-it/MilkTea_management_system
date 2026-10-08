package vn.edu.ute.milktea.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.order.Invoice;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ThanhToan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTT")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaHD", nullable = false, unique = true)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "PhuongThuc", nullable = false, length = 16)
    private PaymentMethod method; // CASH, BANK_TRANSFER

    @Column(name = "SoTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "ThamChieu", length = 120)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiThu", nullable = false)
    private Account recordedBy;

    @Column(name = "ThuLuc", nullable = false)
    private Instant paidAt;

    @Column(name = "MaYeuCau", length = 64)
    private String idempotencyKey;
}
