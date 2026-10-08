package vn.edu.ute.milktea.entity.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "HoaDon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaHD")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false, unique = true)
    private Order order;

    @Column(name = "TongTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "TienGiam", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "PhaiTra", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "TyLeGiamChot", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal snapshotDiscountPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 12)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.HIEU_LUC; // HIEU_LUC, DA_HUY

    @Column(name = "LapLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "HuyLuc")
    private Instant cancelledAt;
}
