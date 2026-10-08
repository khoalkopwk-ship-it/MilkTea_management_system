package vn.edu.ute.milktea.entity.inventory;

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
public class PreparationBatchItemId implements Serializable {

    @Column(name = "MaMe")
    private Long batchId;

    @Column(name = "MaNL")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreparationBatchItemId that = (PreparationBatchItemId) o;
        return Objects.equals(batchId, that.batchId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, materialId);
    }
}
