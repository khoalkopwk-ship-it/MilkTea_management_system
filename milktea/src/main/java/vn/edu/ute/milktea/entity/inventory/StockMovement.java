package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "LichSuKho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBienDong")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(name = "ViTri", nullable = false, length = 8)
    private StockLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "Loai", nullable = false, length = 16)
    private StockMovementType type;

    @Column(name = "LuongBienDong", nullable = false, precision = 18, scale = 3)
    private BigDecimal deltaQuantity;

    @Column(name = "TonTruoc", nullable = false, precision = 18, scale = 3)
    private BigDecimal beforeQuantity;

    @Column(name = "TonSau", nullable = false, precision = 18, scale = 3)
    private BigDecimal afterQuantity;

    @Column(name = "NguonLoai", nullable = false, length = 16)
    private String sourceType; // ORDER, IMPORT, ISSUE, BATCH, WASTE, REMAKE

    @Column(name = "NguonMa", nullable = false)
    private Long sourceId;

    @Column(name = "NguonDong")
    private Long sourceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiLap", nullable = false)
    private Account createdBy;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "LyDo", length = 500)
    private String reason;
}
