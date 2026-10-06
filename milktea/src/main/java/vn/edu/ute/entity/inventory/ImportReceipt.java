package vn.edu.ute.entity.inventory;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_receipts")
public class ImportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 50)
    private String receiptCode;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String note;

    @OneToMany(
        mappedBy = "importReceipt",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<ImportReceiptItem> items = new ArrayList<>();

    public ImportReceipt() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addItem(ImportReceiptItem item) {
        items.add(item);
        item.setImportReceipt(this);
    }

    public void removeItem(ImportReceiptItem item) {
        items.remove(item);
        item.setImportReceipt(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<ImportReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<ImportReceiptItem> items) {
        this.items = items;
    }
}