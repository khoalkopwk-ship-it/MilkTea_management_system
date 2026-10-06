package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.StockIssue;

import java.util.List;
import java.util.Optional;

public interface StockIssueRepository
        extends JpaRepository<StockIssue, Long> {

    Optional<StockIssue> findByIssueCode(String issueCode);

    boolean existsByIssueCode(String issueCode);

    List<StockIssue> findByBranchIdOrderByCreatedAtDesc(Long branchId);
}