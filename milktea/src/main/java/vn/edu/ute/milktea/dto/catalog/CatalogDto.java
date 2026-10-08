package vn.edu.ute.milktea.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class CatalogDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductItem {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String name;
        private String size;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryItem {
        private Long id;
        private String name;
        private Boolean active;
        private List<ProductItem> products;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuResponse {
        private List<CategoryItem> categories;
    }
}
