package vn.edu.ute.dto.inventory;

import java.util.ArrayList;
import java.util.List;

public class PreparationBatchRequest {

    private String batchCode;
    private String note;
    private List<PreparationBatchItemRequest> items = new ArrayList<>();

    public PreparationBatchRequest() {
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<PreparationBatchItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PreparationBatchItemRequest> items) {
        this.items = items;
    }
}