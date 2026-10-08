package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.StockIssueItem;

@Repository
public interface StockIssueItemRepository extends JpaRepository<StockIssueItem, Object> {
}
