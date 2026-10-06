package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.ImportReceiptItem;

import java.util.List;

public interface ImportReceiptItemRepository
        extends JpaRepository<ImportReceiptItem, Long> {

    List<ImportReceiptItem> findByImportReceiptId(Long importReceiptId);
}