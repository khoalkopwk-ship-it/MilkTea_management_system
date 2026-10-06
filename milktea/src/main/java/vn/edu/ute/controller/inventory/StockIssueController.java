package vn.edu.ute.controller.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.ute.dto.inventory.StockIssueItemRequest;
import vn.edu.ute.dto.inventory.StockIssueRequest;
import vn.edu.ute.entity.inventory.Material;
import vn.edu.ute.entity.inventory.StockIssue;
import vn.edu.ute.entity.inventory.StockIssueItem;
import vn.edu.ute.service.inventory.StockIssueService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/issues")
public class StockIssueController {

    private static final Long DEFAULT_BRANCH_ID = 1L;

    private final StockIssueService stockIssueService;

    public StockIssueController(
            StockIssueService stockIssueService
    ) {
        this.stockIssueService = stockIssueService;
    }

    @PostMapping
    public ResponseEntity<StockIssue> createStockIssue(
            @RequestBody StockIssueRequest request
    ) {

        List<StockIssueItem> items = new ArrayList<>();

        for (StockIssueItemRequest itemRequest : request.getItems()) {

            Material material = new Material();
            material.setId(itemRequest.getMaterialId());

            StockIssueItem item = new StockIssueItem();
            item.setMaterial(material);
            item.setQuantity(itemRequest.getQuantity());

            items.add(item);
        }

        StockIssue issue =
                stockIssueService.createStockIssue(
                        DEFAULT_BRANCH_ID,
                        request.getIssueCode(),
                        null,
                        request.getNote(),
                        items
                );

        return ResponseEntity.ok(issue);
    }
}