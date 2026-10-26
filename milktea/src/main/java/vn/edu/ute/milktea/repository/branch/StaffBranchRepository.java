package vn.edu.ute.milktea.repository.branch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.branch.StaffBranch;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffBranchRepository extends JpaRepository<StaffBranch, Long> {

    List<StaffBranch> findByAccountIdAndStatus(Long accountId, String status);

    default List<StaffBranch> findByAccountId(Long accountId) {
        return findByAccountIdAndStatus(accountId, "DANG_LAM");
    }

    Optional<StaffBranch> findByAccountIdAndBranchIdAndStatus(Long accountId, Long branchId, String status);

    List<StaffBranch> findByBranchIdAndStatus(Long branchId, String status);
}
