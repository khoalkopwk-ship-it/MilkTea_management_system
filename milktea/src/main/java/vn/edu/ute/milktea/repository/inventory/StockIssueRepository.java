package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.DocumentStatus;
import vn.edu.ute.milktea.entity.inventory.StockIssue;

import java.util.List;

@Repository
public interface StockIssueRepository extends JpaRepository<StockIssue, Long> {
    List<StockIssue> findByStatus(DocumentStatus status);
    java.util.Optional<StockIssue> findByIdempotencyKey(String key);
}

