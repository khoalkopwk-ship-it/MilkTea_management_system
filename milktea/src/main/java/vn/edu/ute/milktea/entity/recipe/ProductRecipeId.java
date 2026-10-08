package vn.edu.ute.milktea.entity.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecipeId implements Serializable {

    @Column(name = "MaMon")
    private Long productId;

    @Column(name = "MaNL")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductRecipeId that = (ProductRecipeId) o;
        return Objects.equals(productId, that.productId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, materialId);
    }
}
