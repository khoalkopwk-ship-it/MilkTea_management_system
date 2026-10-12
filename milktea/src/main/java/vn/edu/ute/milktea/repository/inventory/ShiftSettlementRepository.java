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
}
