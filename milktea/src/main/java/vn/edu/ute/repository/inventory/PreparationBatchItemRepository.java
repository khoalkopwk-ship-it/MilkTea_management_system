package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.PreparationBatchItem;

import java.util.List;

public interface PreparationBatchItemRepository
        extends JpaRepository<PreparationBatchItem, Long> {

    List<PreparationBatchItem>
    findByPreparationBatchId(Long preparationBatchId);
}