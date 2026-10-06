package vn.edu.ute.entity.inventory;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "stocks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_stock_branch_material_location",
            columnNames = {
                "branch_id",
                "material_id",
                "location"
            }
        )
    }
)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Tạm thời chỉ lưu ID chi nhánh.
     * Mai không tạo Entity Branch vì Branch thuộc phần của Nam.
     */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(name = "location", nullable = false, length = 20)
    private StockLocation location;

    @Column(
        name = "quantity",
        nullable = false,
        precision = 18,
        scale = 3
    )
    private BigDecimal quantity = BigDecimal.ZERO;

    public Stock() {
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

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public StockLocation getLocation() {
        return location;
    }

    public void setLocation(StockLocation location) {
        this.location = location;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}