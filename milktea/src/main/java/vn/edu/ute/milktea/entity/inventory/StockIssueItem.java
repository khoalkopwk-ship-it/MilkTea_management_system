package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietXuat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockIssueItem {

    @EmbeddedId
    private StockIssueItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("issueId")
    @JoinColumn(name = "MaPhieu", nullable = false)
    private StockIssue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "SoLuong", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
}
