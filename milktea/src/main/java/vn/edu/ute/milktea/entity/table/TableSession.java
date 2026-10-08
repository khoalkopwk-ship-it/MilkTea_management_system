package vn.edu.ute.milktea.entity.table;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.time.Instant;

@Entity
@Table(name = "PhienBan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhien")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaBan", nullable = false)
    private DiningTable table;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 8)
    @Builder.Default
    private TableSessionStatus status = TableSessionStatus.OPEN;

    @Column(name = "MoLuc", nullable = false)
    private Instant openedAt;

    @Column(name = "DongLuc")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiMo")
    private Account openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiDong")
    private Account closedBy;

    @Version
    @Column(name = "Version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
