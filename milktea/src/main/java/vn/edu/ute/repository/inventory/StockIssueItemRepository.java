package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.StockIssueItem;

import java.util.List;

public interface StockIssueItemRepository
        extends JpaRepository<StockIssueItem, Long> {

    List<StockIssueItem> findByStockIssueId(Long stockIssueId);
}