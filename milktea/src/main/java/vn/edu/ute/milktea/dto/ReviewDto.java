package vn.edu.ute.milktea.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.review.ReviewMediaType;

import java.time.Instant;
import java.util.List;

public class ReviewDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReviewRequest {
        @NotNull(message = "Mã chi tiết đơn hàng không được để trống")
        private Long orderItemId;

        @NotNull(message = "Số sao đánh giá không được để trống")
        @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
        @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
        private Integer rating;

        @NotBlank(message = "Nội dung đánh giá không được để trống")
        @Size(min = 50, max = 1000, message = "Nội dung đánh giá phải từ 50 đến 1000 ký tự")
        private String content;

        private List<MediaItemRequest> mediaList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaItemRequest {
        @NotBlank(message = "URL media không được để trống")
        private String mediaUrl;

        @Builder.Default
        private ReviewMediaType mediaType = ReviewMediaType.IMAGE;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaItemResponse {
        private Long id;
        private String mediaUrl;
        private ReviewMediaType mediaType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewResponse {
        private Long id;
        private Long orderItemId;
        private Long productId;
        private String productName;
        private Long customerId;
        private String customerName;
        private Long branchId;
        private String branchName;
        private Integer rating;
        private String content;
        private Boolean hidden;
        private String hiddenReason;
        private Instant createdAt;
        private Instant updatedAt;
        private List<MediaItemResponse> mediaList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModerateReviewRequest {
        @NotNull(message = "Trạng thái ẩn không được để trống")
        private Boolean hidden;

        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewListResponse {
        private List<ReviewResponse> items;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private Double averageRating;
        private long totalReviews;
    }
}
