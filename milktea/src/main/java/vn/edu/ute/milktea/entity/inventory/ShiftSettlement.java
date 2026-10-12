package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "KetCaKho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKetCa")
    private Long id;

    @Column(name = "MaCa", length = 64)
    private String shiftCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ViTri", nullable = false, length = 8)
    private StockLocation location; // BEP

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiChot", nullable = false)
    private Account closedBy;

    @Column(name = "ChotLuc", nullable = false)
    private Instant closedAt;

    @Column(name = "TuLuc", nullable = false)
    private Instant fromTime;

    @Column(name = "DenLuc", nullable = false)
    private Instant toTime;

    @Column(name = "TongChiPhiHaoHut", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalLossCost = BigDecimal.ZERO;

    @Column(name = "GhiChu", length = 500)
    private String notes;

    @Column(name = "MaYeuCau", unique = true, length = 64)
    private String idempotencyKey;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShiftSettlementItem> items = new ArrayList<>();
}
