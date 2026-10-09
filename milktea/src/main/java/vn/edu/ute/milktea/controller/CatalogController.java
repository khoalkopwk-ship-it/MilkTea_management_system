package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.CatalogDto;
import vn.edu.ute.milktea.service.catalog.CatalogService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping({"/public/menu", "/catalog/menu"})
    public ResponseEntity<ApiResponse<CatalogDto.MenuResponse>> getMenu() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getMenu()));
    }

    @GetMapping({"/public/categories", "/catalog/categories"})
    public ResponseEntity<ApiResponse<java.util.List<CatalogDto.CategoryItem>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getCategories()));
    }

    @GetMapping({"/public/products", "/catalog/products"})
    public ResponseEntity<ApiResponse<java.util.List<CatalogDto.ProductItem>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getProducts()));
    }

    @GetMapping({"/public/products/{id}", "/catalog/products/{id}"})
    public ResponseEntity<ApiResponse<CatalogDto.ProductItem>> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getProduct(id)));
    }
}
