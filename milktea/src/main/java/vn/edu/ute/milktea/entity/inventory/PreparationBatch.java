package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "MeSoChe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreparationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaMe")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCTSC", nullable = false)
    private PreparationRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNLDauRa", nullable = false)
    private Material outputMaterial;

    @Column(name = "LuongDuKien", nullable = false, precision = 18, scale = 3)
    private BigDecimal expectedQuantity;

    @Column(name = "LuongThucThu", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal actualQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 12)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.NHAP;

    @Column(name = "LyDoChenhLech", length = 500)
    private String discrepancyReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiLap", nullable = false)
    private Account createdBy;

    @Column(name = "GhiSoLuc")
    private Instant postedAt;

    @Column(name = "MaYeuCau", unique = true, length = 64)
    private String idempotencyKey;
}
