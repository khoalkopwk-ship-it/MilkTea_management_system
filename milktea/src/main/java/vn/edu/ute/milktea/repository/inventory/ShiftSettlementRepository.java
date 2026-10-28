package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.milktea.entity.inventory.ShiftSettlement;
import vn.edu.ute.milktea.entity.inventory.StockLocation;

import java.util.List;
import java.util.Optional;

public interface ShiftSettlementRepository extends JpaRepository<ShiftSettlement, Long> {
    List<ShiftSettlement> findByLocationOrderByClosedAtDesc(StockLocation location);
    List<ShiftSettlement> findAllByOrderByClosedAtDesc();
    List<ShiftSettlement> findByClosedAtBetweenOrderByClosedAtDesc(java.time.Instant from, java.time.Instant to);
    List<ShiftSettlement> findByLocationAndClosedAtBetweenOrderByClosedAtDesc(StockLocation location, java.time.Instant from, java.time.Instant to);
    Optional<ShiftSettlement> findByIdempotencyKey(String idempotencyKey);

    @org.springframework.data.jpa.repository.Query("""
        SELECT s FROM ShiftSettlement s
        WHERE s.closedAt >= :from AND s.closedAt < :to
          AND (:branchId IS NULL OR s.branch.id = :branchId)
        ORDER BY s.closedAt DESC
        """)
    List<ShiftSettlement> findByClosedAtBetweenAndBranchId(
            @org.springframework.data.repository.query.Param("from") java.time.Instant from,
            @org.springframework.data.repository.query.Param("to") java.time.Instant to,
            @org.springframework.data.repository.query.Param("branchId") Long branchId
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT s FROM ShiftSettlement s
        WHERE (:branchId IS NULL OR s.branch.id = :branchId)
        ORDER BY s.closedAt DESC
        """)
    List<ShiftSettlement> findAllByBranchIdOrderByClosedAtDesc(
            @org.springframework.data.repository.query.Param("branchId") Long branchId
    );
}
