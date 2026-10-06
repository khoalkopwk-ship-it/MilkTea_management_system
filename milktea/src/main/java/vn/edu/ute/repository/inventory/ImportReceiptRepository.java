package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.ImportReceipt;

import java.util.List;
import java.util.Optional;

public interface ImportReceiptRepository
        extends JpaRepository<ImportReceipt, Long> {

    Optional<ImportReceipt> findByReceiptCode(String receiptCode);

    boolean existsByReceiptCode(String receiptCode);

    List<ImportReceipt> findByBranchIdOrderByCreatedAtDesc(Long branchId);
}