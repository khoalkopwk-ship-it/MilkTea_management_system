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
public class StockIssueItemId implements Serializable {

    @Column(name = "MaPhieu")
    private Long issueId;

    @Column(name = "MaNL")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockIssueItemId that = (StockIssueItemId) o;
        return Objects.equals(issueId, that.issueId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, materialId);
    }
}
