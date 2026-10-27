package vn.edu.ute.milktea.repository.voucher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.voucher.VoucherBranch;
import vn.edu.ute.milktea.entity.voucher.VoucherBranchId;

import java.util.List;

@Repository
public interface VoucherBranchRepository extends JpaRepository<VoucherBranch, VoucherBranchId> {

    List<VoucherBranch> findByIdVoucherId(Long voucherId);

    List<VoucherBranch> findByIdBranchId(Long branchId);

    boolean existsByIdVoucherIdAndIdBranchId(Long voucherId, Long branchId);

    @Modifying
    @Query("DELETE FROM VoucherBranch vb WHERE vb.id.voucherId = :voucherId")
    void deleteByVoucherId(@Param("voucherId") Long voucherId);
}
