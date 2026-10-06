package vn.edu.ute.entity.inventory;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "preparation_batch_items")
public class PreparationBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preparation_batch_id", nullable = false)
    private PreparationBatch preparationBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_role", nullable = false, length = 20)
    private PreparationItemRole itemRole;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    public PreparationBatchItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PreparationBatch getPreparationBatch() {
        return preparationBatch;
    }

    public void setPreparationBatch(PreparationBatch preparationBatch) {
        this.preparationBatch = preparationBatch;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public PreparationItemRole getItemRole() {
        return itemRole;
    }

    public void setItemRole(PreparationItemRole itemRole) {
        this.itemRole = itemRole;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}