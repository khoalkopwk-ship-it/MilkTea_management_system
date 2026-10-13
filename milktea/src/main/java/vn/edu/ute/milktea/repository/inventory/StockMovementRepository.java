
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

    @Query("""
        SELECT m
        FROM StockMovement m
        WHERE m.type = :type
          AND m.sourceType = 'WASTE'
          AND m.createdAt >= :start
          AND m.createdAt < :end
        ORDER BY m.createdAt ASC
        """)
    List<StockMovement> findManualWasteBetween(
            @Param("type") StockMovementType type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COALESCE(SUM(m.lossCost), 0)
        FROM StockMovement m
        WHERE m.type = :type
          AND m.sourceType = 'WASTE'
          AND m.createdAt >= :start
          AND m.createdAt < :end
        """)
    BigDecimal sumManualWasteCost(
            @Param("type") StockMovementType type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COUNT(m) FROM StockMovement m
        WHERE m.type = :type
          AND m.sourceType = 'WASTE'
          AND m.lossCost IS NULL
          AND m.createdAt >= :start
          AND m.createdAt < :end
        """)
    long countUnpricedManualWaste(
            @Param("type") StockMovementType type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
