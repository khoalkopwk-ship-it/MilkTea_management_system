package vn.edu.ute.entity.inventory;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_issues")
public class StockIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "issue_code", nullable = false, unique = true, length = 50)
    private String issueCode;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String note;

    @OneToMany(
        mappedBy = "stockIssue",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<StockIssueItem> items = new ArrayList<>();

    public StockIssue() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addItem(StockIssueItem item) {
        items.add(item);
        item.setStockIssue(this);
    }

    public void removeItem(StockIssueItem item) {
        items.remove(item);
        item.setStockIssue(null);
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

    public String getIssueCode() {
        return issueCode;
    }

    public void setIssueCode(String issueCode) {
        this.issueCode = issueCode;
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

    public List<StockIssueItem> getItems() {
        return items;
    }

    public void setItems(List<StockIssueItem> items) {
        this.items = items;
    }
}