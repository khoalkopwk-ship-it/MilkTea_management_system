package vn.edu.ute.dto.inventory;

import java.util.ArrayList;
import java.util.List;

public class StockIssueRequest {

    private String issueCode;
    private String note;
    private List<StockIssueItemRequest> items = new ArrayList<>();

    public StockIssueRequest() {
    }

    public String getIssueCode() {
        return issueCode;
    }

    public void setIssueCode(String issueCode) {
        this.issueCode = issueCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<StockIssueItemRequest> getItems() {
        return items;
    }

    public void setItems(List<StockIssueItemRequest> items) {
        this.items = items;
    }
}