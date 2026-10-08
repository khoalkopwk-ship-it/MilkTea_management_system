package vn.edu.ute.milktea.entity.order;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.catalog.Product;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietDonHang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTiet")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMon", nullable = false)
    private Product product;

    @Column(name = "TenMonChot", nullable = false, length = 120)
    private String snapshotName;

    @Column(name = "SizeChot", nullable = false, length = 12)
    private String snapshotSize;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity;

    @Column(name = "DonGiaChot", nullable = false, precision = 18, scale = 2)
    private BigDecimal snapshotUnitPrice;

    @Column(name = "GhiChu", length = 500)
    private String note;
}
