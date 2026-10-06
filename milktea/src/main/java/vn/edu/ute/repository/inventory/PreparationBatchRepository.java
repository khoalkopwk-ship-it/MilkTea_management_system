package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.PreparationBatch;

import java.util.List;
import java.util.Optional;

public interface PreparationBatchRepository
        extends JpaRepository<PreparationBatch, Long> {

    Optional<PreparationBatch> findByBatchCode(String batchCode);

    boolean existsByBatchCode(String batchCode);

    List<PreparationBatch>
    findByBranchIdOrderByCreatedAtDesc(Long branchId);
}