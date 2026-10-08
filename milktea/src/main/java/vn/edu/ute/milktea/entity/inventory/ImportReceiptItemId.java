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
public class ImportReceiptItemId implements Serializable {

    @Column(name = "MaPhieu")
    private Long receiptId;

    @Column(name = "MaNL")
    private Long materialId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportReceiptItemId that = (ImportReceiptItemId) o;
        return Objects.equals(receiptId, that.receiptId) && Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptId, materialId);
    }
}
