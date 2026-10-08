package vn.edu.ute.milktea.entity.order;

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
public class OrderIngredientSnapshotId implements Serializable {

    @Column(name = "MaDH")
    private Long orderId;

    @Column(name = "MaNL")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderIngredientSnapshotId that = (OrderIngredientSnapshotId) o;
        return Objects.equals(orderId, that.orderId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, materialId);
    }
}
