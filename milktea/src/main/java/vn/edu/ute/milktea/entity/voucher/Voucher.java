package vn.edu.ute.milktea.entity.voucher;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "Voucher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaVoucher")
    private Long id;

    @Column(name = "MaCode", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "TenVoucher", nullable = false, length = 150)
    private String name;

    @Column(name = "MoTa", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiGiamGia", nullable = false, length = 20)
    private DiscountType discountType; // PERCENT, FIXED_AMOUNT

    @Column(name = "GiaTriGiam", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "GiamToiDa", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "GiaTriDonToiThieu", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "TongSoLuot", nullable = false)
    private Integer totalUsageLimit;

    @Column(name = "SoLuotDaDung", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "SoLuotMoiKhach", nullable = false)
    @Builder.Default
    private Integer usageLimitPerCustomer = 1;

    @Column(name = "ApDungToanChuoi", nullable = false)
    @Builder.Default
    private Boolean applyAllBranches = true;

    @Column(name = "NgayBatDau", nullable = false)
    private Instant startDate;

    @Column(name = "NgayKetThuc", nullable = false)
    private Instant endDate;

    @Column(name = "KichHoat", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "TaoLuc", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "CapNhatLuc", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public boolean isExpired() {
        Instant now = Instant.now();
        return now.isBefore(startDate) || now.isAfter(endDate);
    }

    public boolean hasRemainingUses() {
        return usedCount < totalUsageLimit;
    }
}
