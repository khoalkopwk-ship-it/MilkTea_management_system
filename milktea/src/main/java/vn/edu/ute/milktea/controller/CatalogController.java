package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.CatalogDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.catalog.CatalogService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller xử lý danh mục, menu, tìm kiếm, lọc, chi tiết món và top 4 danh sách nổi bật (CUS-01, CUS-02, CUS-03).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping({"/public/menu", "/catalog/menu"})
    public ResponseEntity<ApiResponse<CatalogDto.MenuResponse>> getMenu(
            @RequestParam(value = "branchId", required = false) Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getMenu(branchId)));
    }

    @GetMapping({"/public/categories", "/catalog/categories"})
    public ResponseEntity<ApiResponse<List<CatalogDto.CategoryItem>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getCategories()));
    }

    @GetMapping({"/public/products", "/catalog/products"})
    public ResponseEntity<ApiResponse<List<CatalogDto.ProductItem>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getProducts()));
    }

    @GetMapping({"/public/products/{id}", "/catalog/products/{id}"})
    public ResponseEntity<ApiResponse<CatalogDto.ProductItem>> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getProduct(id)));
    }

    /**
     * Tìm kiếm và lọc sản phẩm phân trang chuẩn backend (CUS-01).
     */
    @GetMapping({"/public/products/search", "/catalog/products/search", "/catalog/search"})
    public ResponseEntity<ApiResponse<CatalogDto.ProductSearchResponse>> searchProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sortBy", required = false, defaultValue = "newest") String sortBy,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {
        String searchKw = (keyword != null && !keyword.isBlank()) ? keyword : q;
        return ResponseEntity.ok(ApiResponse.ok(
                catalogService.searchProducts(searchKw, categoryId, minPrice, maxPrice, sortBy, branchId, page, size)));
    }

    /**
     * Phân trang thực đơn chuẩn backend (12 món đại diện / trang, tìm kiếm, lọc, bộ sưu tập) (Mục 4.4).
     */
    @GetMapping({"/public/products/menu-page", "/catalog/menu-page"})
    public ResponseEntity<ApiResponse<CatalogDto.MenuPageResponse>> getMenuPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sortBy", required = false, defaultValue = "default") String sortBy,
            @RequestParam(value = "featuredType", required = false) String featuredType,
            @RequestParam(value = "collection", required = false) String collection,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {
        String searchKw = (keyword != null && !keyword.isBlank()) ? keyword : q;
        String fType = (featuredType != null && !featuredType.isBlank()) ? featuredType : ((collection != null && !collection.isBlank()) ? collection : "all");
        return ResponseEntity.ok(ApiResponse.ok(
                catalogService.getPaginatedMenu(searchKw, categoryId, minPrice, maxPrice, sortBy, fType, branchId, page, size)));
    }

    /**
     * Chi tiết món uống, size, topping, điểm đánh giá và trạng thái yêu thích (CUS-03).
     */
    @GetMapping({"/public/products/{id}/detail", "/catalog/products/{id}/detail"})
    public ResponseEntity<ApiResponse<CatalogDto.ProductDetailResponse>> getProductDetail(
            @PathVariable("id") Long id,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getProductDetail(id, branchId, actor)));
    }

    /**
     * Bốn danh sách nổi bật: Mới nhất, Bán chạy nhất, Đánh giá cao nhất, Yêu thích nhất (CUS-02 - Top 20).
     */
    @GetMapping({"/public/products/featured", "/catalog/products/featured", "/catalog/featured"})
    public ResponseEntity<ApiResponse<CatalogDto.FeaturedProductsResponse>> getFeaturedProducts(
            @RequestParam(value = "type", defaultValue = "newest") String type,
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getFeaturedProducts(type, branchId, limit)));
    }

    @GetMapping({"/catalog/top-selling", "/public/top-selling"})
    public ResponseEntity<ApiResponse<CatalogDto.FeaturedProductsResponse>> getTopSelling(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getFeaturedProducts("best_seller", branchId, limit)));
    }

    @GetMapping({"/catalog/newest", "/public/newest"})
    public ResponseEntity<ApiResponse<CatalogDto.FeaturedProductsResponse>> getNewest(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getFeaturedProducts("newest", branchId, limit)));
    }

    private CurrentActor resolveActor(CurrentActor principalActor, HttpServletRequest request) {
        if (principalActor != null) {
            return principalActor;
        }
        if (request != null) {
            Object attr = request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
            if (attr instanceof CurrentActor ca) {
                return ca;
            }
        }
        return null;
    }
}
