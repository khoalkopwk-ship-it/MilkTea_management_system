package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.DocumentStatus;
import vn.edu.ute.milktea.entity.inventory.PreparationBatch;

import java.util.List;

@Repository
public interface PreparationBatchRepository extends JpaRepository<PreparationBatch, Long> {
    List<PreparationBatch> findByStatus(DocumentStatus status);
}
