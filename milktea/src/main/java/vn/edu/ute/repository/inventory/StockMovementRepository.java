package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.StockMovement;

import java.util.List;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<StockMovement> findByReferenceTypeAndReferenceId(
            String referenceType,
            Long referenceId
    );

    boolean existsByReferenceTypeAndReferenceId(
            String referenceType,
            Long referenceId
    );
}