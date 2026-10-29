package vn.edu.ute.milktea.dto;

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
        private Boolean allowTopping;
        private String name;
        private String size;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Boolean active;
        @Builder.Default
        private boolean available = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryItem {
        private Long id;
        private String name;
        private Boolean active;
        private Boolean allowTopping;
        private List<ProductItem> products;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuResponse {
        private List<CategoryItem> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetailResponse {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private Boolean allowTopping;
        private String name;
        private String size;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Boolean active;
        @Builder.Default
        private boolean available = true;
        private Long branchId;
        private String branchName;
        private Double averageRating;
        private long reviewCount;
        @Builder.Default
        private boolean isFavorite = false;
        private List<String> availableSizes;
        private List<SizeOption> sizeOptions;
        private List<ProductItem> toppings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeOption {
        private Long id;
        private String size;
        private BigDecimal price;
        @Builder.Default
        private boolean available = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSearchResponse {
        private List<ProductItem> items;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturedProductsResponse {
        private String type; // newest, best_seller, top_rated, most_favorited
        private Long branchId;
        private String branchName;
        private List<ProductItem> products;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuPageResponse {
        private List<ProductItem> items;
        private int page;
        private int pageSize;
        private long totalItems;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
