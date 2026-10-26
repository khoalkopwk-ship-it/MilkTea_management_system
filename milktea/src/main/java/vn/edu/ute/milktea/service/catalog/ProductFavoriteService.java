package vn.edu.ute.milktea.service.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.FavoriteDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.catalog.ProductFavorite;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.catalog.ProductFavoriteRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.security.CurrentActor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service quản lý danh sách sản phẩm yêu thích (Wishlist - CUS-05).
 */
@Service
@RequiredArgsConstructor
public class ProductFavoriteService {

    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final BranchProductRepository branchProductRepository;

    @Transactional
    public FavoriteDto.ToggleFavoriteResponse toggleFavorite(Long productId, CurrentActor actor) {
        validateAuthenticatedCustomer(actor);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw BusinessException.badRequest(ErrorCode.PRODUCT_UNAVAILABLE, "Món uống hiện không hoạt động");
        }

        Optional<ProductFavorite> existingFav = productFavoriteRepository.findByAccountIdAndProductId(actor.getAccountId(), productId);
        boolean isFavorite;
        String message;

        if (existingFav.isPresent()) {
            productFavoriteRepository.delete(existingFav.get());
            isFavorite = false;
            message = "Đã xóa món khỏi danh sách yêu thích";
        } else {
            Account account = accountRepository.findById(actor.getAccountId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy tài khoản"));

            ProductFavorite newFav = ProductFavorite.builder()
                    .account(account)
                    .product(product)
                    .build();
            productFavoriteRepository.save(newFav);
            isFavorite = true;
            message = "Đã thêm món vào danh sách yêu thích";
        }

        long totalFavorites = productFavoriteRepository.countByProductId(productId);

        return FavoriteDto.ToggleFavoriteResponse.builder()
                .productId(productId)
                .isFavorite(isFavorite)
                .totalFavorites(totalFavorites)
                .message(message)
                .build();
    }

    @Transactional(readOnly = true)
    public FavoriteDto.FavoriteListResponse getFavorites(CurrentActor actor, Long branchId, Pageable pageable) {
        validateAuthenticatedCustomer(actor);

        Page<ProductFavorite> favoritePage = productFavoriteRepository.findByAccountIdOrderByCreatedAtDesc(actor.getAccountId(), pageable);

        Map<Long, Boolean> availabilityMap = new HashMap<>();
        if (branchId != null) {
            branchProductRepository.findByBranchId(branchId).forEach(bp -> {
                if (bp.getProduct() != null) {
                    availabilityMap.put(bp.getProduct().getId(), Boolean.TRUE.equals(bp.getAvailable()));
                }
            });
        }

        var items = favoritePage.getContent().stream().map(fav -> {
            Product p = fav.getProduct();
            boolean available = branchId == null || availabilityMap.getOrDefault(p.getId(), true);
            return FavoriteDto.FavoriteItem.builder()
                    .id(fav.getId())
                    .productId(p.getId())
                    .productName(p.getName())
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                    .size(p.getSize())
                    .price(p.getPrice())
                    .imageUrl(p.getImageUrl())
                    .createdAt(fav.getCreatedAt())
                    .available(available)
                    .build();
        }).toList();

        return FavoriteDto.FavoriteListResponse.builder()
                .items(items)
                .page(favoritePage.getNumber())
                .size(favoritePage.getSize())
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long productId, CurrentActor actor) {
        if (actor == null || !actor.isAuthenticated() || actor.getAccountId() == null) {
            return false;
        }
        return productFavoriteRepository.existsByAccountIdAndProductId(actor.getAccountId(), productId);
    }

    @Transactional(readOnly = true)
    public long countFavorites(Long productId) {
        return productFavoriteRepository.countByProductId(productId);
    }

    private void validateAuthenticatedCustomer(CurrentActor actor) {
        if (actor == null || !actor.isAuthenticated() || actor.getAccountId() == null) {
            throw BusinessException.unauthorized(ErrorCode.ACCESS_DENIED, "Vui lòng đăng nhập để sử dụng tính năng danh sách yêu thích");
        }
    }
}
