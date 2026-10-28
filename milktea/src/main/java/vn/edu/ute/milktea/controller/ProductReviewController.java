package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.ReviewDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.review.ProductReviewService;

/**
 * Controller xử lý đánh giá, bình luận sản phẩm và kiểm duyệt (CUS-06).
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;
    private final vn.edu.ute.milktea.service.media.ProductImageService productImageService;

    @PostMapping(value = "/media/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> uploadReviewMedia(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        if (actor == null || !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CUSTOMER)) {
            throw vn.edu.ute.milktea.common.BusinessException.forbidden(
                    vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED,
                    "Chỉ khách hàng mới có thể tải ảnh đánh giá");
        }
        String mediaUrl = productImageService.uploadReviewImage(file);
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("mediaUrl", mediaUrl)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDto.ReviewResponse>> createReview(
            @Valid @RequestBody ReviewDto.CreateReviewRequest req,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        return ResponseEntity.ok(ApiResponse.ok(productReviewService.createReview(req, actor)));
    }

    @GetMapping({"/product/{productId}", "/public/product/{productId}"})
    public ResponseEntity<ApiResponse<ReviewDto.ReviewListResponse>> getProductReviews(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        int validSize = Math.min(Math.max(size, 1), 50);
        int validPage = Math.max(page, 0);
        return ResponseEntity.ok(ApiResponse.ok(
                productReviewService.getProductReviews(productId, PageRequest.of(validPage, validSize))));
    }

    @PutMapping("/{id}/moderation")
    public ResponseEntity<ApiResponse<ReviewDto.ReviewResponse>> moderateReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody ReviewDto.ModerateReviewRequest req,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        return ResponseEntity.ok(ApiResponse.ok(productReviewService.moderateReview(reviewId, req, actor)));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<ReviewDto.ReviewListResponse>> getBranchReviews(
            @PathVariable("branchId") Long branchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        int validSize = Math.min(Math.max(size, 1), 50);
        int validPage = Math.max(page, 0);
        return ResponseEntity.ok(ApiResponse.ok(
                productReviewService.getReviewsForModeration(branchId, actor, PageRequest.of(validPage, validSize))));
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
