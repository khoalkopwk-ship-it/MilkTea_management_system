package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.milktea.entity.inventory.ShiftSettlementItem;

import java.util.List;

public interface ShiftSettlementItemRepository extends JpaRepository<ShiftSettlementItem, Long> {
    List<ShiftSettlementItem> findBySettlementId(Long settlementId);
}
