package vn.edu.ute.controller.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.ute.entity.inventory.Stock;
import vn.edu.ute.entity.inventory.StockLocation;
import vn.edu.ute.service.inventory.StockAlertService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/alerts")
public class StockAlertController {

    private static final Long DEFAULT_BRANCH_ID = 1L;

    private final StockAlertService stockAlertService;

    public StockAlertController(
            StockAlertService stockAlertService
    ) {
        this.stockAlertService = stockAlertService;
    }

    @GetMapping
    public ResponseEntity<List<Stock>> getLowStocks() {

        List<Stock> stocks =
                stockAlertService.getLowStock(
                        DEFAULT_BRANCH_ID
                );

        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/kitchen")
    public ResponseEntity<List<Stock>> getLowKitchenStocks() {

        List<Stock> stocks =
                stockAlertService.getLowKitchenStock(
                        DEFAULT_BRANCH_ID
                );

        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkLowStock(
            @RequestParam Long materialId,
            @RequestParam StockLocation location
    ) {

        boolean lowStock =
                stockAlertService.isLowStock(
                        DEFAULT_BRANCH_ID,
                        materialId,
                        location
                );

        return ResponseEntity.ok(
                Map.of("lowStock", lowStock)
        );
    }
}