package vn.edu.ute.entity.inventory;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "import_receipt_items")
public class ImportReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_receipt_id", nullable = false)
    private ImportReceipt importReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    public ImportReceiptItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportReceipt getImportReceipt() {
        return importReceipt;
    }

    public void setImportReceipt(ImportReceipt importReceipt) {
        this.importReceipt = importReceipt;
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