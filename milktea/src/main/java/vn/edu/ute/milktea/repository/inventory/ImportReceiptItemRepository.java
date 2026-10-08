package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.ImportReceiptItem;
import vn.edu.ute.milktea.entity.inventory.ImportReceiptItemId;

import java.util.List;

@Repository
public interface ImportReceiptItemRepository extends JpaRepository<ImportReceiptItem, ImportReceiptItemId> {
    List<ImportReceiptItem> findByIdReceiptId(Long receiptId);
}
