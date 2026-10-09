package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/issues")
    public ResponseEntity<ApiResponse<Void>> createIssue(
            @Valid @RequestBody InventoryDto.CreateStockIssueRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.recordIssue(requestBody, kitchen);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
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
    public ResponseEntity<ApiResponse<Void>> createPreparationBatch(
            @Valid @RequestBody InventoryDto.CreatePreparationBatchRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.recordPreparation(requestBody, kitchen);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
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

    @PutMapping("/stocks/threshold")
    public ResponseEntity<ApiResponse<Void>> updateThreshold(
            @Valid @RequestBody InventoryDto.UpdateThresholdRequest requestBody) {

        inventoryService.configureThreshold(requestBody);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
