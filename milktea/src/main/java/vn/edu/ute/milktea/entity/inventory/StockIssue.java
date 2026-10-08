package vn.edu.ute.milktea.entity.inventory;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.time.Instant;

@Entity
@Table(name = "PhieuXuat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhieu")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiLap", nullable = false)
    private Account createdBy;

    @Column(name = "LyDo", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "TrangThai", nullable = false, length = 12)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.NHAP;

    @Column(name = "LapLuc", nullable = false)
    private Instant createdAt;

    @Column(name = "GhiSoLuc")
    private Instant postedAt;

    @Column(name = "MaYeuCau", unique = true, length = 64)
    private String idempotencyKey;
}
