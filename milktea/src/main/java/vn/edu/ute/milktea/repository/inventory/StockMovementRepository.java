
package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.inventory.StockMovement;
import vn.edu.ute.milktea.entity.inventory.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByMaterialIdAndLocationOrderByCreatedAtDesc(
            Long materialId,
            StockLocation location
    );

    List<StockMovement> findTop100ByOrderByCreatedAtDesc();

    List<StockMovement> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<StockMovement> findByBranchIdAndMaterialIdAndLocationOrderByCreatedAtDesc(
            Long branchId,
            Long materialId,
            StockLocation location
    );

    @Query("""
        SELECT m
        FROM StockMovement m
        WHERE m.type = :type
          AND m.sourceType = 'WASTE'
          AND m.createdAt >= :start
          AND m.createdAt < :end
          AND (:branchId IS NULL OR m.branch.id = :branchId)
        ORDER BY m.createdAt ASC
        """)
    List<StockMovement> findManualWasteBetween(
            @Param("type") StockMovementType type,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("branchId") Long branchId
    );

    default List<StockMovement> findManualWasteBetween(StockMovementType type, Instant start, Instant end) {
        return findManualWasteBetween(type, start, end, null);
    }

    @Query("""
        SELECT COALESCE(SUM(m.lossCost), 0)
        FROM StockMovement m
        WHERE m.type = :type
          AND m.sourceType = 'WASTE'
          AND m.createdAt >= :start
          AND m.createdAt < :end
          AND (:branchId IS NULL OR m.branch.id = :branchId)
        """)
    BigDecimal sumManualWasteCost(
            @Param("type") StockMovementType type,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("branchId") Long branchId
    );

    default BigDecimal sumManualWasteCost(StockMovementType type, Instant start, Instant end) {
        return sumManualWasteCost(type, start, end, null);
    }

    @Query("""
        SELECT COUNT(m) FROM StockMovement m
        WHERE m.type = :type
          AND m.sourceType = 'WASTE'
          AND m.lossCost IS NULL
          AND m.createdAt >= :start
          AND m.createdAt < :end
          AND (:branchId IS NULL OR m.branch.id = :branchId)
        """)
    long countUnpricedManualWaste(
            @Param("type") StockMovementType type,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("branchId") Long branchId
    );

    default long countUnpricedManualWaste(StockMovementType type, Instant start, Instant end) {
        return countUnpricedManualWaste(type, start, end, null);
    }
}
