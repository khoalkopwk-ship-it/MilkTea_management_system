package vn.edu.ute.milktea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class FavoriteDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteItem {
        private Long id;
        private Long productId;
        private String productName;
        private String categoryName;
        private String size;
        private BigDecimal price;
        private String imageUrl;
        private Instant createdAt;
        @Builder.Default
        private boolean available = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoriteListResponse {
        private List<FavoriteItem> items;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleFavoriteResponse {
        private Long productId;
        private boolean isFavorite;
        private long totalFavorites;
        private String message;
    }
}
