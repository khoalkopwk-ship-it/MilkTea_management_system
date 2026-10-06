package vn.edu.ute.controller.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.ute.dto.inventory.ImportReceiptItemRequest;
import vn.edu.ute.dto.inventory.ImportReceiptRequest;
import vn.edu.ute.dto.inventory.StockResponse;
import vn.edu.ute.entity.inventory.ImportReceipt;
import vn.edu.ute.entity.inventory.ImportReceiptItem;
import vn.edu.ute.entity.inventory.Material;
import vn.edu.ute.service.inventory.InventoryService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Long DEFAULT_BRANCH_ID = 1L;

    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService
    ) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stocks")
    public ResponseEntity<List<StockResponse>> getStocks() {

        List<StockResponse> stocks =
                inventoryService.getStocks(
                        DEFAULT_BRANCH_ID
                );

        return ResponseEntity.ok(stocks);
    }

    @PostMapping("/imports")
    public ResponseEntity<ImportReceipt> createImport(
            @RequestBody ImportReceiptRequest request
    ) {

        List<ImportReceiptItem> items =
                new ArrayList<>();

        for (ImportReceiptItemRequest itemRequest
                : request.getItems()) {

            Material material = new Material();
            material.setId(
                    itemRequest.getMaterialId()
            );

            ImportReceiptItem item =
                    new ImportReceiptItem();

            item.setMaterial(material);
            item.setQuantity(
                    itemRequest.getQuantity()
            );

            items.add(item);
        }

        ImportReceipt receipt =
                inventoryService.createImportReceipt(
                        DEFAULT_BRANCH_ID,
                        request.getReceiptCode(),
                        null,
                        request.getNote(),
                        items
                );

        return ResponseEntity.ok(receipt);
    }
}