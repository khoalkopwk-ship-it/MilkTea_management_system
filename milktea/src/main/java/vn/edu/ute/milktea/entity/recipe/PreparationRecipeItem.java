package vn.edu.ute.milktea.entity.recipe;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.inventory.Material;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietCTSoChe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreparationRecipeItem {

    @EmbeddedId
    private PreparationRecipeItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("recipeId")
    @JoinColumn(name = "MaCTSC", nullable = false)
    private PreparationRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material inputMaterial;

    @Column(name = "LuongDauVaoChuan", nullable = false, precision = 18, scale = 3)
    private BigDecimal standardInputQuantity;
}
