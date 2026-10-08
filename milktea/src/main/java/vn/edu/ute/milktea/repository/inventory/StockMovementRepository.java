package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.inventory.StockMovement;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByMaterialIdAndLocationOrderByCreatedAtDesc(Long materialId, StockLocation location);
    List<StockMovement> findTop100ByOrderByCreatedAtDesc();
}
