package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.admin.AdminService;
import vn.edu.ute.milktea.service.inventory.InventoryService;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final AdminService adminService;
    private final AccountRepository accountRepository;

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<List<InventoryDto.StockItemResponse>>> getStocks(
            @RequestParam(value = "location", defaultValue = "BEP") StockLocation location) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getStocks(location)));
    }

    @GetMapping("/stocks/summary")
    public ResponseEntity<ApiResponse<InventoryDto.StockSummaryResponse>> getStockSummary(
            @RequestParam(value = "location", required = false) StockLocation location,
            @RequestParam(value = "alertFilter", required = false) String alertFilter,
            @RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getStocksWithFilter(location, alertFilter, query)));
    }


    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<Void>> createBatch(
            @Valid @RequestBody InventoryDto.CreatePreparationBatchRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.recordPreparation(requestBody, kitchen);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }

    
    @PostMapping("/preparation-batches")
    @org.springframework.security.access.prepost.PreAuthorize(
            "hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<Void>> createPreparationBatch(
            @RequestHeader(value = "Idempotency-Key", required = false)
            String key,
            @Valid @RequestBody
            InventoryDto.CreatePreparationBatchRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null
                ? accountRepository.findById(actor.getAccountId()).orElse(null)
                : null;

        inventoryService.recordPreparation(requestBody, kitchen, key);

        return ResponseEntity.status(201)
                .body(ApiResponse.ok(null));
    }

    @GetMapping("/preparation-batches")
    @PreAuthorize("hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<List<InventoryDto.PreparationBatchResponse>>> listPreparationBatches(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listPreparationBatches(from, to, all)));
    }

    
    @PostMapping("/imports")
    public ResponseEntity<ApiResponse<Void>> createImport(
            @Valid @RequestBody InventoryDto.CreateImportReceiptRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.recordImport(requestBody, account);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }

    @GetMapping("/imports")
    public ResponseEntity<ApiResponse<List<InventoryDto.ImportReceiptResponse>>> listImports() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listImports()));
    }

    @PostMapping("/waste")
    public ResponseEntity<ApiResponse<Void>> createWaste(
            @Valid @RequestBody InventoryDto.CreateWasteRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.recordWaste(requestBody, account);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<InventoryDto.StockMovementResponse>>> listMovements(
            @RequestParam(value = "materialId", required = false) Long materialId,
            @RequestParam(value = "location", required = false) StockLocation location) {

        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listMovements(materialId, location)));
    }

    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<AdminDto.MaterialResponse>>> getMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listMaterials()));
    }

    @GetMapping("/preparation-recipes")
    public ResponseEntity<ApiResponse<List<AdminDto.PreparationRecipeResponse>>> getPreparationRecipes() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPreparationRecipes()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/stocks/threshold")
    public ResponseEntity<ApiResponse<Void>> updateThreshold(
            @Valid @RequestBody InventoryDto.UpdateThresholdRequest requestBody) {

        inventoryService.configureThreshold(requestBody);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // === KẾT CA KHO & HAO HỤT ===

    @GetMapping("/shift-settlement/preview")
    @PreAuthorize("hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryDto.ShiftSettlementPreviewResponse>> getShiftSettlementPreview(
            @RequestParam(value = "location", defaultValue = "BEP") StockLocation location) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getShiftSettlementPreview(location)));
    }

    @PostMapping("/shift-settlement")
    @PreAuthorize("hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryDto.ShiftSettlementResponse>> createShiftSettlement(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InventoryDto.CreateShiftSettlementRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        InventoryDto.ShiftSettlementResponse response = inventoryService.recordShiftSettlement(requestBody, account, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @GetMapping("/shift-settlement")
    @PreAuthorize("hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<List<InventoryDto.ShiftSettlementResponse>>> listShiftSettlements(
            @RequestParam(value = "location", required = false) StockLocation location,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listShiftSettlements(location, from, to, all)));
    }

    @GetMapping("/shift-settlement/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryDto.ShiftSettlementResponse>> getShiftSettlement(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getShiftSettlement(id)));
    }
}
