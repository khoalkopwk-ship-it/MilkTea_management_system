package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockId implements Serializable {

    @Column(name = "MaNL")
    private Long materialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ViTri", length = 8)
    private StockLocation location;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockId stockId = (StockId) o;
        return Objects.equals(materialId, stockId.materialId) && location == stockId.location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(materialId, location);
    }
}
