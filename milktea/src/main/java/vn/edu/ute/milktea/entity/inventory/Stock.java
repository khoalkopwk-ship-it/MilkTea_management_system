package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "TonNguyenLieu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @EmbeddedId
    private StockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "SoLuong", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "NguongCanhBao", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal threshold = BigDecimal.ZERO;

    @Version
    @Column(name = "Version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
