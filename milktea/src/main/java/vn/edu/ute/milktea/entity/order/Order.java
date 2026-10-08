package vn.edu.ute.milktea.entity.order;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.table.TableSession;

import java.time.Instant;

@Entity
@Table(name = "DonHang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDH")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaPhien")
    private TableSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "Nguon", nullable = false, length = 10)
    private OrderSource source; // TABLE, COUNTER

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 24)
    private OrderStatus status; // CHO_XAC_NHAN, CHO_CHE_BIEN, DANG_CHE_BIEN, HOAN_THANH, DA_HUY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ThayTheMaDH")
    private Order replacementOfOrder;

    @Column(name = "MaYeuCau", length = 64)
    private String idempotencyKey;

    @Column(name = "GhiChu", length = 500)
    private String note;

    @Column(name = "TaoLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "BatDauLuc")
    private Instant startedAt;

    @Column(name = "HoanThanhLuc")
    private Instant completedAt;

    @Version
    @Column(name = "Version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
