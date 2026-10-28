package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.admin.AdminService;
import vn.edu.ute.milktea.service.inventory.InventoryService;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.milktea.service.media.ProductImageService;

import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.service.branch.BranchService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final InventoryService inventoryService;
    private final ProductImageService productImageService;
    private final vn.edu.ute.milktea.service.cancellation.RefundService refundService;
    private final BranchService branchService;

    private Long resolveManagerBranchId(CurrentActor actor) {
        return resolveManagerBranchId(actor, null);
    }

    private Long resolveManagerBranchId(CurrentActor actor, Long branchIdParam) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            if (actor.getBranchId() == null) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý cơ sở chưa được phân công chi nhánh hoạt động");
            }
            return actor.getBranchId();
        }
        return branchIdParam;
    }

    // ==========================================
    // 1. QUẢN LÝ NHÂN VIÊN
    // ==========================================
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AdminDto.StaffResponse>>> listStaffAccounts(
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveManagerBranchId(actor, branchIdParam);
        return ResponseEntity.ok(ApiResponse.ok(adminService.listStaffAccounts(branchId)));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> getStaffAccount(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStaffAccount(id, branchId)));
    }

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> createStaffAccount(
            @Valid @RequestBody AdminDto.CreateStaffRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createStaffAccount(request, branchId)));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> updateStaffAccount(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.UpdateStaffRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        Long currentAdminId = actor != null ? actor.getAccountId() : null;
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateStaffAccount(id, request, currentAdminId, branchId)));
    }

    @PostMapping(value = "/accounts/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            adminService.getStaffAccount(id, resolveManagerBranchId(actor));
        }
        String avatarUrl = productImageService.uploadAvatarImage(id, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("avatarUrl", avatarUrl)));
    }

    @PostMapping("/accounts/{id}/toggle-status")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> toggleStaffStatus(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Boolean> body,
            @AuthenticationPrincipal CurrentActor actor) {
        Long currentAdminId = actor != null ? actor.getAccountId() : null;
        Long branchId = resolveManagerBranchId(actor);
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(ApiResponse.ok(adminService.toggleStaffStatus(id, active, currentAdminId, branchId)));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaffAccount(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        Long currentAdminId = actor != null ? actor.getAccountId() : null;
        Long branchId = resolveManagerBranchId(actor);
        adminService.deleteStaffAccount(id, currentAdminId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 2. QUẢN LÝ DANH MỤC (CHỈ ADMIN HỆ THỐNG - F18)
    // ==========================================
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<AdminDto.CategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCategories()));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<AdminDto.CategoryResponse>> getCategory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getCategory(id)));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<AdminDto.CategoryResponse>> createCategory(
            @Valid @RequestBody AdminDto.CategoryRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && !actor.hasRole(Role.ADMIN)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chỉ Quản trị viên hệ thống (ADMIN) mới có quyền tạo danh mục món");
        }
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<AdminDto.CategoryResponse>> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.CategoryRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && !actor.hasRole(Role.ADMIN)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chỉ Quản trị viên hệ thống (ADMIN) mới có quyền sửa danh mục món");
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateCategory(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && !actor.hasRole(Role.ADMIN)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chỉ Quản trị viên hệ thống (ADMIN) mới có quyền xóa danh mục món");
        }
        adminService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 3. QUẢN LÝ SẢN PHẨM & CÔNG THỨC
    // ==========================================
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Object>> listProducts(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = resolveManagerBranchId(actor);
        if (page != null || limit != null || categoryId != null || active != null || (query != null && !query.isBlank())) {
            int p = page != null ? page : 1;
            int l = limit != null ? limit : 10;
            return ResponseEntity.ok(ApiResponse.ok(adminService.searchProducts(categoryId, active, query, p, l, branchId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.listProducts(branchId)));
    }

    @GetMapping("/documents/refunds/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingRefundsCount(
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = (actor != null && actor.hasRole(Role.MANAGER)) ? actor.getBranchId() : null;
        long count = refundService.getRefunds(vn.edu.ute.milktea.entity.cancellation.RefundStatus.CHO_DUYET, branchId).size();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("pendingCount", count)));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<AdminDto.ProductResponse>> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getProduct(id)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<AdminDto.ProductResponse>> createProduct(
            @Valid @RequestBody AdminDto.ProductRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý cơ sở không có quyền thêm món mới");
        }
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createProduct(request)));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<AdminDto.ProductResponse>> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.ProductRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý cơ sở không có quyền chỉnh sửa tên, size, giá bán của món");
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateProduct(id, request)));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý cơ sở không có quyền xóa món");
        }
        adminService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/products/{id}/recipe")
    public ResponseEntity<ApiResponse<AdminDto.ProductRecipeResponse>> getProductRecipe(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getProductRecipe(id)));
    }

    @PutMapping("/products/{id}/recipe")
    public ResponseEntity<ApiResponse<AdminDto.ProductRecipeResponse>> updateProductRecipe(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.UpdateProductRecipeRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý cơ sở không có quyền chỉnh sửa công thức món");
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateProductRecipe(id, request)));
    }

    @PostMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminDto.ProductImageView>> uploadProductImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý cơ sở không có quyền đổi ảnh món");
        }
        return ResponseEntity.ok(ApiResponse.ok(productImageService.uploadProductImage(id, file)));
    }

    @PostMapping("/products/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleProductStatus(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Boolean> body,
            @AuthenticationPrincipal CurrentActor actor) {
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            branchService.updateProductAvailability(resolveManagerBranchId(actor), id, active, actor);
        } else {
            adminService.toggleProductStatus(id, active);
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 4. QUẢN LÝ BÀN
    // ==========================================
    private void checkNotAdminForTables(CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.ADMIN)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản trị viên toàn chuỗi không quản lý sơ đồ bàn và mã QR");
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<AdminDto.TableResponse>>> listTables(
            @AuthenticationPrincipal CurrentActor actor) {
        checkNotAdminForTables(actor);
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.ok(ApiResponse.ok(adminService.listTables(branchId)));
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<AdminDto.TableResponse>> getTable(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        checkNotAdminForTables(actor);
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTable(id, branchId)));
    }

    @PostMapping("/tables")
    public ResponseEntity<ApiResponse<AdminDto.TableResponse>> createTable(
            @Valid @RequestBody AdminDto.TableRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        checkNotAdminForTables(actor);
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createTable(request, branchId)));
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<AdminDto.TableResponse>> updateTable(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.TableRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        checkNotAdminForTables(actor);
        Long branchId = resolveManagerBranchId(actor);
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateTable(id, request, branchId)));
    }

    @DeleteMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        checkNotAdminForTables(actor);
        Long branchId = resolveManagerBranchId(actor);
        adminService.deleteTable(id, branchId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/tables/{id}/qr")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTableQr(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        checkNotAdminForTables(actor);
        Long branchId = resolveManagerBranchId(actor);
        String qr = adminService.getTableQr(id, branchId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("qrCode", qr)));
    }

    // ==========================================
    // 5. QUẢN LÝ NGUYÊN LIỆU
    // ==========================================
    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<AdminDto.MaterialResponse>>> listMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listMaterials()));
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<AdminDto.MaterialResponse>> getMaterial(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getMaterial(id)));
    }

    @PostMapping("/materials")
    public ResponseEntity<ApiResponse<AdminDto.MaterialResponse>> createMaterial(
            @Valid @RequestBody AdminDto.MaterialRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createMaterial(request)));
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<AdminDto.MaterialResponse>> updateMaterial(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.MaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateMaterial(id, request)));
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chi nhánh không có quyền xóa nguyên liệu");
        }
        adminService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 6. QUẢN LÝ CÔNG THỨC SƠ CHẾ
    // ==========================================
    @GetMapping("/preparation-recipes")
    public ResponseEntity<ApiResponse<List<AdminDto.PreparationRecipeResponse>>> listPreparationRecipes(
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chi nhánh không có quyền xem cấu hình định mức sơ chế");
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPreparationRecipes()));
    }

    @GetMapping("/preparation-recipes/{id}")
    public ResponseEntity<ApiResponse<AdminDto.PreparationRecipeResponse>> getPreparationRecipe(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chi nhánh không có quyền xem cấu hình định mức sơ chế");
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPreparationRecipe(id)));
    }

    @PostMapping("/preparation-recipes")
    public ResponseEntity<ApiResponse<AdminDto.PreparationRecipeResponse>> createPreparationRecipe(
            @Valid @RequestBody AdminDto.CreatePreparationRecipeRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chi nhánh không có quyền cấu hình định mức sơ chế");
        }
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createPreparationRecipe(request)));
    }

    @PutMapping("/preparation-recipes/{id}")
    public ResponseEntity<ApiResponse<AdminDto.PreparationRecipeResponse>> updatePreparationRecipe(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.CreatePreparationRecipeRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chi nhánh không có quyền cấu hình định mức sơ chế");
        }
        return ResponseEntity.ok(ApiResponse.ok(adminService.updatePreparationRecipe(id, request)));
    }

    @DeleteMapping("/preparation-recipes/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePreparationRecipe(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Quản lý chi nhánh không có quyền cấu hình định mức sơ chế");
        }
        adminService.deletePreparationRecipe(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 7. CẤU HÌNH NGƯỠNG CẢNH BÁO TỒN KHO THEO VỊ TRÍ
    // ==========================================
    @PutMapping("/inventory/stocks/{materialId}/{location}/threshold")
    public ResponseEntity<ApiResponse<Void>> updateStockThreshold(
            @PathVariable("materialId") Long materialId,
            @PathVariable("location") StockLocation location,
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @RequestBody Map<String, BigDecimal> body,
            @AuthenticationPrincipal CurrentActor actor) {

        Long branchId = (actor != null && actor.hasRole(Role.MANAGER))
                ? resolveManagerBranchId(actor)
                : (branchIdParam != null ? branchIdParam : branchService.getOrCreateCentralWarehouse().getId());
        BigDecimal threshold = body.getOrDefault("threshold", BigDecimal.ZERO);
        inventoryService.configureThreshold(new InventoryDto.UpdateThresholdRequest(branchId, materialId, location, threshold), branchId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
