package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.PreparationBatchItem;
import vn.edu.ute.milktea.entity.inventory.PreparationBatchItemId;

import java.util.List;

@Repository
public interface PreparationBatchItemRepository extends JpaRepository<PreparationBatchItem, PreparationBatchItemId> {
    List<PreparationBatchItem> findByIdBatchId(Long batchId);
}
