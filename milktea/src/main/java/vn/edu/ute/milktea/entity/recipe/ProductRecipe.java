package vn.edu.ute.milktea.entity.recipe;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.Material;

import java.math.BigDecimal;

@Entity
@Table(name = "CongThucMon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecipe {

    @EmbeddedId
    private ProductRecipeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "MaMon", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "DinhLuong", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
}
