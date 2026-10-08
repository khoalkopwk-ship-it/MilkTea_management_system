package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.PreparationBatchItem;

@Repository
public interface PreparationBatchItemRepository extends JpaRepository<PreparationBatchItem, Object> {
}
