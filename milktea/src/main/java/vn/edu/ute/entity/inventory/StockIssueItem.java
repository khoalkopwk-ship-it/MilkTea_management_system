package vn.edu.ute.entity.inventory;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_issue_items")
public class StockIssueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_issue_id", nullable = false)
    private StockIssue stockIssue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    public StockIssueItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockIssue getStockIssue() {
        return stockIssue;
    }

    public void setStockIssue(StockIssue stockIssue) {
        this.stockIssue = stockIssue;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}