package vn.edu.ute.dto.inventory;

import java.util.ArrayList;
import java.util.List;

public class ImportReceiptRequest {

    private String receiptCode;
    private String note;
    private List<ImportReceiptItemRequest> items = new ArrayList<>();

    public ImportReceiptRequest() {
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<ImportReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ImportReceiptItemRequest> items) {
        this.items = items;
    }
}