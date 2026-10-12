
package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.DocumentStatus;
import vn.edu.ute.milktea.entity.inventory.PreparationBatch;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreparationBatchRepository
        extends JpaRepository<PreparationBatch, Long> {

    List<PreparationBatch> findByStatus(DocumentStatus status);

    List<PreparationBatch> findByOutputMaterialIdAndStatus(Long outputMaterialId, DocumentStatus status);

    List<PreparationBatch> findByOutputMaterialIdAndStatusAndPostedAtBetween(
            Long outputMaterialId, DocumentStatus status, java.time.Instant from, java.time.Instant to);

    List<PreparationBatch> findAllByOrderByPostedAtDescIdDesc();

    List<PreparationBatch> findByPostedAtBetweenOrderByPostedAtDescIdDesc(java.time.Instant from, java.time.Instant to);

    Optional<PreparationBatch> findByIdempotencyKey(
            String idempotencyKey);
}
