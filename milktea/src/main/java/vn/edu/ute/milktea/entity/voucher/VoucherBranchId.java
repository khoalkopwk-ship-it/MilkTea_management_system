package vn.edu.ute.milktea.entity.voucher;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VoucherBranchId implements Serializable {

    @Column(name = "MaVoucher")
    private Long voucherId;

    @Column(name = "MaCN")
    private Long branchId;
}
