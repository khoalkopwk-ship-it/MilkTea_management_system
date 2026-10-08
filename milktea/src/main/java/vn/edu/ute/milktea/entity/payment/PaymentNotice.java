package vn.edu.ute.milktea.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.order.Invoice;

import java.time.Instant;

@Entity
@Table(name = "ThongBaoThanhToan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTB")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaHD", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "PhuongThuc", nullable = false, length = 16)
    private PaymentMethod method;

    @Column(name = "ThamChieu", length = 120)
    private String reference;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "MaYeuCau", length = 64)
    private String idempotencyKey;
}
