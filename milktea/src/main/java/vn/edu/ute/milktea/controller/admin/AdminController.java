package vn.edu.ute.milktea.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.admin.AdminDto;
import vn.edu.ute.milktea.dto.inventory.InventoryDto;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.admin.AdminService;
import vn.edu.ute.milktea.service.inventory.InventoryService;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.milktea.service.media.ProductImageService;

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

    // ==========================================
    // 1. QUẢN LÝ NHÂN VIÊN
    // ==========================================
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AdminDto.StaffResponse>>> listStaffAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listStaffAccounts()));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> getStaffAccount(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStaffAccount(id)));
    }

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> createStaffAccount(
            @Valid @RequestBody AdminDto.CreateStaffRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createStaffAccount(request)));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<AdminDto.StaffResponse>> updateStaffAccount(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.UpdateStaffRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateStaffAccount(id, request)));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaffAccount(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CurrentActor actor) {
        Long currentAdminId = actor != null ? actor.getAccountId() : null;
        adminService.deleteStaffAccount(id, currentAdminId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 2. QUẢN LÝ DANH MỤC
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
            @Valid @RequestBody AdminDto.CategoryRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<AdminDto.CategoryResponse>> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateCategory(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("id") Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 3. QUẢN LÝ SẢN PHẨM & CÔNG THỨC
    // ==========================================
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<AdminDto.ProductResponse>>> listProducts() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listProducts()));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<AdminDto.ProductResponse>> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getProduct(id)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<AdminDto.ProductResponse>> createProduct(
            @Valid @RequestBody AdminDto.ProductRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createProduct(request)));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<AdminDto.ProductResponse>> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateProduct(id, request)));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Long id) {
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
            @Valid @RequestBody AdminDto.UpdateProductRecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateProductRecipe(id, request)));
    }

    @PostMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminDto.ProductImageView>> uploadProductImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(productImageService.uploadProductImage(id, file)));
    }

    // ==========================================
    // 4. QUẢN LÝ BÀN
    // ==========================================
    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<AdminDto.TableResponse>>> listTables() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listTables()));
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<AdminDto.TableResponse>> getTable(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTable(id)));
    }

    @PostMapping("/tables")
    public ResponseEntity<ApiResponse<AdminDto.TableResponse>> createTable(
            @Valid @RequestBody AdminDto.TableRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createTable(request)));
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<AdminDto.TableResponse>> updateTable(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.TableRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateTable(id, request)));
    }

    @DeleteMapping("/tables/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable("id") Long id) {
        adminService.deleteTable(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/tables/{id}/qr")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTableQr(@PathVariable("id") Long id) {
        String qr = adminService.getTableQr(id);
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
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(@PathVariable("id") Long id) {
        adminService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ==========================================
    // 6. QUẢN LÝ CÔNG THỨC SƠ CHẾ
    // ==========================================
    @GetMapping("/preparation-recipes")
    public ResponseEntity<ApiResponse<List<AdminDto.PreparationRecipeResponse>>> listPreparationRecipes() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPreparationRecipes()));
    }

    @GetMapping("/preparation-recipes/{id}")
    public ResponseEntity<ApiResponse<AdminDto.PreparationRecipeResponse>> getPreparationRecipe(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPreparationRecipe(id)));
    }

    @PostMapping("/preparation-recipes")
    public ResponseEntity<ApiResponse<AdminDto.PreparationRecipeResponse>> createPreparationRecipe(
            @Valid @RequestBody AdminDto.CreatePreparationRecipeRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(adminService.createPreparationRecipe(request)));
    }

    @PutMapping("/preparation-recipes/{id}")
    public ResponseEntity<ApiResponse<AdminDto.PreparationRecipeResponse>> updatePreparationRecipe(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminDto.CreatePreparationRecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updatePreparationRecipe(id, request)));
    }

    @DeleteMapping("/preparation-recipes/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePreparationRecipe(@PathVariable("id") Long id) {
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
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal threshold = body.getOrDefault("threshold", BigDecimal.ZERO);
        inventoryService.configureThreshold(new InventoryDto.UpdateThresholdRequest(materialId, location, threshold));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
