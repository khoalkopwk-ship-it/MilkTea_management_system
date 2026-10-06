package vn.edu.ute.dto.inventory;

import java.math.BigDecimal;

public class ImportReceiptItemRequest {

    private Long materialId;
    private BigDecimal quantity;

    public ImportReceiptItemRequest() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}