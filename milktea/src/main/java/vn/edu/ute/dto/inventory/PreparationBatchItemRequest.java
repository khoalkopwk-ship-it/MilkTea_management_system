package vn.edu.ute.dto.inventory;

import java.math.BigDecimal;

public class PreparationBatchItemRequest {

    private Long materialId;
    private String itemRole;
    private BigDecimal quantity;

    public PreparationBatchItemRequest() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getItemRole() {
        return itemRole;
    }

    public void setItemRole(String itemRole) {
        this.itemRole = itemRole;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}