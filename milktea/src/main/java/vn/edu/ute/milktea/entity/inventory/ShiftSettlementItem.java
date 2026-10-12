package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietKetCaKho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaChiTiet")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKetCa", nullable = false)
    private ShiftSettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNL", nullable = false)
    private Material material;

    @Column(name = "DonVi", nullable = false, length = 16)
    private String unit;

    @Column(name = "TonSo", nullable = false, precision = 18, scale = 3)
    private BigDecimal bookQuantity;

    @Column(name = "TonThucTe", nullable = false, precision = 18, scale = 3)
    private BigDecimal actualQuantity;

    @Column(name = "LuongLoaiBo", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal discardedQuantity = BigDecimal.ZERO;

    @Column(name = "LuongGiuLai", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal retainedQuantity = BigDecimal.ZERO;

    @Column(name = "ChenhLechKiemKe", nullable = false, precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal unexplainedDifference = BigDecimal.ZERO;

    @Column(name = "GiaVonDonVi", nullable = false, precision = 18, scale = 6)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "ChiPhiHaoHut", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal lossCost = BigDecimal.ZERO;

    @Column(name = "LyDo", length = 500)
    private String reason;
}
