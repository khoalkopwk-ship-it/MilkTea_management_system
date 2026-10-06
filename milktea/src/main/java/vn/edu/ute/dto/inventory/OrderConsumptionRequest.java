package vn.edu.ute.dto.inventory;

import java.util.ArrayList;
import java.util.List;

public class OrderConsumptionRequest {

    private Long orderId;
    private Long branchId;
    private List<OrderConsumptionItem> items = new ArrayList<>();

    public OrderConsumptionRequest() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public List<OrderConsumptionItem> getItems() {
        return items;
    }

    public void setItems(List<OrderConsumptionItem> items) {
        this.items = items;
    }
}