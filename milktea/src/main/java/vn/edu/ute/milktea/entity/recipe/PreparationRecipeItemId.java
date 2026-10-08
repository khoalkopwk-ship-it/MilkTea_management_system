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
public class PreparationRecipeItemId implements Serializable {

    @Column(name = "MaCTSC")
    private Long recipeId;

    @Column(name = "MaNL")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreparationRecipeItemId that = (PreparationRecipeItemId) o;
        return Objects.equals(recipeId, that.recipeId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeId, materialId);
    }
}
