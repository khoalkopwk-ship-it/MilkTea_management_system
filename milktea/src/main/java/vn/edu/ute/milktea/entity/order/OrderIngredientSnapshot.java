package vn.edu.ute.milktea.entity.order;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.inventory.Material;
import vn.edu.ute.milktea.entity.inventory.MaterialType;

import java.math.BigDecimal;

@Entity
@Table(name = "DinhMucDonHang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIngredientSnapshot {

    @EmbeddedId
    private OrderIngredientSnapshotId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orderId")
    @JoinColumn(name = "MaDH", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "TongLuong", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "LoaiChot", nullable = false, length = 8)
    private MaterialType snapshotType;

    @Column(name = "DonViChot", nullable = false, length = 16)
    private String snapshotUnit;
}
