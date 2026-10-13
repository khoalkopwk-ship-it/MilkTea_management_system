
package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.ute.milktea.entity.inventory.ShiftSettlementItem;

import java.time.Instant;
import java.util.List;

public interface ShiftSettlementItemRepository
        extends JpaRepository<ShiftSettlementItem, Long> {

    List<ShiftSettlementItem> findBySettlementId(Long settlementId);

    // Dashboard: Top nguyên liệu hao hụt ngoài định mức khi kết ca
    @Query("""
        SELECT i.material.id,
               i.material.name,
               i.unit,
               SUM(i.discardedQuantity),
               SUM(i.lossCost)
        FROM ShiftSettlementItem i
        JOIN i.settlement s
        WHERE s.closedAt >= :start
          AND s.closedAt < :end
          AND i.discardedQuantity > 0
        GROUP BY i.material.id, i.material.name, i.unit
        ORDER BY SUM(i.lossCost) DESC, i.material.name ASC
        """)
    List<Object[]> findTopWasteMaterials(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );
    
@Query("""
    SELECT i.material.id,
           i.material.name,
           i.unit,
           SUM(i.discardedQuantity),
           SUM(i.lossCost)
    FROM ShiftSettlementItem i
    JOIN i.settlement s
    WHERE s.closedAt >= :start
      AND s.closedAt < :end
      AND i.discardedQuantity > 0
    GROUP BY i.material.id, i.material.name, i.unit
    """)
List<Object[]> findAllWasteMaterialsBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
);

}
