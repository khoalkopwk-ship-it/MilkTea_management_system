package vn.edu.ute.milktea.entity.order;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.catalog.Product;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietDonHangTopping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemTopping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTietTopping")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaChiTiet", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaMonTopping", nullable = false)
    private Product toppingProduct;

    @Column(name = "TenToppingChot", nullable = false, length = 120)
    private String snapshotName;

    @Column(name = "DonGiaToppingChot", nullable = false, precision = 18, scale = 2)
    private BigDecimal snapshotUnitPrice;
}
