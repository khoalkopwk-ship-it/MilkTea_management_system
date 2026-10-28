package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.inventory.MaterialType;
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
    private final vn.edu.ute.milktea.service.branch.BranchService branchService;

    private Long resolveBranchScope(CurrentActor actor, Long requestedBranchId) {
        if (actor == null) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Yêu cầu đăng nhập");
        }
        if (actor.isSystemAdmin()) {
            return requestedBranchId;
        }
        Long staffBranchId = actor.getBranchId();
        if (staffBranchId == null && (actor.getAssignedBranchIds() == null || actor.getAssignedBranchIds().isEmpty())) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Tài khoản nhân viên chưa được gán chi nhánh hoạt động");
        }
        if (requestedBranchId != null) {
            if (!actor.canAccessBranch(requestedBranchId)) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không thể truy cập dữ liệu của chi nhánh khác");
            }
            return requestedBranchId;
        }
        return staffBranchId;
    }

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<List<InventoryDto.StockItemResponse>>> getStocks(
            @RequestParam(value = "location", defaultValue = "BEP") StockLocation location,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getStocks(location, branchId)));
    }

    @GetMapping("/stocks/summary")
    public ResponseEntity<ApiResponse<InventoryDto.StockSummaryResponse>> getStockSummary(
            @RequestParam(value = "location", required = false) StockLocation location,
            @RequestParam(value = "type", required = false) MaterialType type,
            @RequestParam(value = "alertFilter", required = false) String alertFilter,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getStocksWithFilter(location, type, alertFilter, query, branchId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/stocks/adjust")
    public ResponseEntity<ApiResponse<Void>> adjustStock(
            @Valid @RequestBody InventoryDto.AdjustStockRequest requestBody,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        boolean isManager = actor != null && actor.hasRole(vn.edu.ute.milktea.entity.account.Role.MANAGER);
        Long branchId = resolveBranchScope(actor, branchIdParam);
        if (isManager) {
            // Quản lý cơ sở chỉ được điều chỉnh ngưỡng tối thiểu; không đổi tên, tồn kho, hay giá vốn
            requestBody.setName(null);
            requestBody.setQuantity(null);
            requestBody.setAverageImportPrice(null);
        }
        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        inventoryService.adjustRawMaterialStock(requestBody, account, branchId, isManager);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/preparation-batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<ApiResponse<Void>> createPreparationBatch(
            @RequestHeader(value = "Idempotency-Key", required = false)
            String key,
            @Valid @RequestBody
            InventoryDto.CreatePreparationBatchRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account kitchen = actor != null
                ? accountRepository.findById(actor.getAccountId()).orElse(null)
                : null;

        Long branchId = resolveBranchScope(actor, requestBody.getBranchId());
        requestBody.setBranchId(branchId);

        inventoryService.recordPreparation(requestBody, kitchen, key);

        return ResponseEntity.status(201)
                .body(ApiResponse.ok(null));
    }

    @GetMapping("/preparation-batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<ApiResponse<List<InventoryDto.PreparationBatchResponse>>> listPreparationBatches(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listPreparationBatches(from, to, all, branchId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/imports")
    public ResponseEntity<ApiResponse<InventoryDto.ImportReceiptResponse>> createImport(
            @Valid @RequestBody InventoryDto.CreateImportReceiptRequest requestBody,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentActor actor) {

        Long branchId = resolveBranchScope(actor, branchIdParam);
        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        var response = inventoryService.recordImport(requestBody, account, branchId, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @GetMapping("/imports")
    public ResponseEntity<ApiResponse<List<InventoryDto.ImportReceiptResponse>>> listImports(
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listImports(branchId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/waste")
    public ResponseEntity<ApiResponse<Void>> createWaste(
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @Valid @RequestBody InventoryDto.CreateWasteRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        Long branchId;
        if (actor != null && !actor.isSystemAdmin()) {
            branchId = resolveBranchScope(actor, branchIdParam);
        } else {
            // ADMIN: áp dụng cho Kho Hàng Tổng hoặc branch được chỉ định
            branchId = (branchIdParam != null) ? branchIdParam : branchService.getOrCreateCentralWarehouse().getId();
        }
        inventoryService.recordWaste(requestBody, account, branchId);
        return ResponseEntity.status(201).body(ApiResponse.ok(null));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<InventoryDto.StockMovementResponse>>> listMovements(
            @RequestParam(value = "materialId", required = false) Long materialId,
            @RequestParam(value = "location", required = false) StockLocation location,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {

        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listMovements(materialId, location, branchId)));
    }

    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<AdminDto.MaterialResponse>>> getMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listMaterials()));
    }

    @GetMapping("/preparation-recipes")
    public ResponseEntity<ApiResponse<List<AdminDto.PreparationRecipeResponse>>> getPreparationRecipes() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPreparationRecipes()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/stocks/threshold")
    public ResponseEntity<ApiResponse<Void>> updateThreshold(
            @Valid @RequestBody InventoryDto.UpdateThresholdRequest requestBody,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {

        Long branchId;
        if (actor != null && !actor.isSystemAdmin()) {
            branchId = resolveBranchScope(actor, branchIdParam);
        } else {
            branchId = (branchIdParam != null) ? branchIdParam
                    : (requestBody.getBranchId() != null ? requestBody.getBranchId() : branchService.getOrCreateCentralWarehouse().getId());
        }
        inventoryService.configureThreshold(requestBody, branchId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // === KẾT CA KHO & HAO HỤT ===

    @GetMapping({"/shift-settlement/preview", "/shift-settlements/preview"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryDto.ShiftSettlementPreviewResponse>> getShiftSettlementPreview(
            @RequestParam(value = "location", defaultValue = "BEP") StockLocation location,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getShiftSettlementPreview(location, branchId)));
    }

    @PostMapping({"/shift-settlement", "/shift-settlements"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryDto.ShiftSettlementResponse>> createShiftSettlement(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InventoryDto.CreateShiftSettlementRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Account account = actor != null ? accountRepository.findById(actor.getAccountId()).orElse(null) : null;
        Long branchId = resolveBranchScope(actor, requestBody.getBranchId());
        requestBody.setBranchId(branchId);
        InventoryDto.ShiftSettlementResponse response = inventoryService.recordShiftSettlement(requestBody, account, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @GetMapping({"/shift-settlement", "/shift-settlements"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<ApiResponse<List<InventoryDto.ShiftSettlementResponse>>> listShiftSettlements(
            @RequestParam(value = "location", required = false) StockLocation location,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveBranchScope(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.listShiftSettlements(location, from, to, all, branchId)));
    }

    @GetMapping({"/shift-settlement/{id}", "/shift-settlements/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<ApiResponse<InventoryDto.ShiftSettlementResponse>> getShiftSettlement(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getShiftSettlement(id, actor)));
    }
}
