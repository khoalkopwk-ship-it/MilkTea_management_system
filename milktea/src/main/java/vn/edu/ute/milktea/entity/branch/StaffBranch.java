package vn.edu.ute.milktea.entity.branch;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.account.Account;

import java.time.Instant;

@Entity
@Table(name = "NhanVienChiNhanh", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_NVCN_TK_CN", columnNames = {"MaTK", "MaCN"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaNVCN")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaTK", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCN", nullable = false)
    private Branch branch;

    @Column(name = "VaiTroChiNhanh", nullable = false, length = 30)
    private String branchRole;

    @Column(name = "TrangThai", nullable = false, length = 30)
    @Builder.Default
    private String status = "HOAT_DONG";

    @Column(name = "NgayPhanCong", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    public boolean isActive() {
        return "HOAT_DONG".equalsIgnoreCase(status);
    }
}
