package vn.edu.ute.milktea.entity.voucher;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.milktea.entity.branch.Branch;

import java.time.Instant;

@Entity
@Table(name = "VoucherChiNhanh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherBranch {

    @EmbeddedId
    private VoucherBranchId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("voucherId")
    @JoinColumn(name = "MaVoucher")
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("branchId")
    @JoinColumn(name = "MaCN")
    private Branch branch;

    @Column(name = "TaoLuc", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
