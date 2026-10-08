package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietMeSoChe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreparationBatchItem {

    @EmbeddedId
    private PreparationBatchItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("batchId")
    @JoinColumn(name = "MaMe", nullable = false)
    private PreparationBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "LuongDinhMuc", nullable = false, precision = 18, scale = 3)
    private BigDecimal recipeQuantity;

    @Column(name = "LuongThucDung", nullable = false, precision = 18, scale = 3)
    private BigDecimal actualQuantity;
}
