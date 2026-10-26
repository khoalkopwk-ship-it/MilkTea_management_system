package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.FavoriteDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.catalog.ProductFavoriteService;

/**
 * Controller xử lý danh sách yêu thích cá nhân (CUS-05).
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class ProductFavoriteController {

    private final ProductFavoriteService productFavoriteService;

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<FavoriteDto.ToggleFavoriteResponse>> toggleFavorite(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        return ResponseEntity.ok(ApiResponse.ok(productFavoriteService.toggleFavorite(productId, actor)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<FavoriteDto.ToggleFavoriteResponse>> removeFavorite(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        return ResponseEntity.ok(ApiResponse.ok(productFavoriteService.toggleFavorite(productId, actor)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FavoriteDto.FavoriteListResponse>> getFavorites(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        int validSize = Math.min(Math.max(size, 1), 50);
        int validPage = Math.max(page, 0);
        return ResponseEntity.ok(ApiResponse.ok(productFavoriteService.getFavorites(actor, branchId, PageRequest.of(validPage, validSize))));
    }

    @GetMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {
        CurrentActor actor = resolveActor(principalActor, request);
        return ResponseEntity.ok(ApiResponse.ok(productFavoriteService.isFavorite(productId, actor)));
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
