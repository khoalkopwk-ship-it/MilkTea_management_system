package vn.edu.ute.milktea.repository.branch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.branch.BranchProduct;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchProductRepository extends JpaRepository<BranchProduct, Long> {

    Optional<BranchProduct> findByBranchIdAndProductId(Long branchId, Long productId);

    List<BranchProduct> findByBranchId(Long branchId);

    List<BranchProduct> findByBranchIdAndActiveTrue(Long branchId);
}
