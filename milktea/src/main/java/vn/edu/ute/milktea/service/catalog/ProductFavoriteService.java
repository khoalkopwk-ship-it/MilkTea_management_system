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
import vn.edu.ute.milktea.entity.account.Role;
import java.math.BigDecimal;
import java.util.*;

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

        List<ProductFavorite> existingFavs = new ArrayList<>();
        List<ProductFavorite> found = productFavoriteRepository.findAllByAccountIdAndProductName(
                actor.getAccountId(), product.getName()
        );
        if (found != null && !found.isEmpty()) {
            existingFavs.addAll(found);
        } else {
            productFavoriteRepository.findByAccountIdAndProductId(actor.getAccountId(), productId)
                    .ifPresent(existingFavs::add);
        }

        boolean isFavorite;
        String message;

        if (!existingFavs.isEmpty()) {
            for (ProductFavorite fav : existingFavs) {
                productFavoriteRepository.delete(fav);
            }
            isFavorite = false;
            message = "Đã xóa món khỏi danh sách yêu thích";
        } else {
            Account account = accountRepository.findById(actor.getAccountId()).orElse(null);
            if (account == null) {
                account = Account.builder().id(actor.getAccountId()).build();
            }

            // Đại diện cho món bằng biến thể có giá thấp nhất (size bé nhất)
            List<Product> sameName = productRepository.findByNameIgnoreCaseAndActiveTrue(product.getName());
            Product representative = (sameName != null && !sameName.isEmpty())
                    ? sameName.stream().min(Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo))).orElse(product)
                    : product;

            ProductFavorite newFav = ProductFavorite.builder()
                    .account(account)
                    .product(representative)
                    .build();
            productFavoriteRepository.save(newFav);
            isFavorite = true;
            message = "Đã thêm món vào danh sách yêu thích";
        }

        long totalFavorites = productFavoriteRepository.countDistinctAccountsByProductName(product.getName());
        if (totalFavorites == 0) {
            totalFavorites = productFavoriteRepository.countByProductId(productId);
        }

        return FavoriteDto.ToggleFavoriteResponse.builder()
                .productId(productId)
                .isFavorite(isFavorite)
                .totalFavorites(totalFavorites)
                .message(message)
                .build();
    }

    @Transactional
    public FavoriteDto.ToggleFavoriteResponse removeFavorite(Long productId, CurrentActor actor) {
        validateAuthenticatedCustomer(actor);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        List<ProductFavorite> existingFavs = productFavoriteRepository.findAllByAccountIdAndProductName(
                actor.getAccountId(), product.getName()
        );

        if (!existingFavs.isEmpty()) {
            productFavoriteRepository.deleteAll(existingFavs);
        }

        long totalFavorites = productFavoriteRepository.countDistinctAccountsByProductName(product.getName());

        return FavoriteDto.ToggleFavoriteResponse.builder()
                .productId(productId)
                .isFavorite(false)
                .totalFavorites(totalFavorites)
                .message("Đã xóa món khỏi danh sách yêu thích")
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

        // Đảm bảo không trùng tên món (nếu CSDL cũ có nhiều size)
        Set<String> seenNames = new HashSet<>();
        List<FavoriteDto.FavoriteItem> items = new ArrayList<>();

        for (ProductFavorite fav : favoritePage.getContent()) {
            Product p = fav.getProduct();
            if (p == null) continue;
            String normalizedName = p.getName().trim().toLowerCase();
            if (seenNames.contains(normalizedName)) {
                continue;
            }
            seenNames.add(normalizedName);

            // Tìm size bé nhất của món này để hiển thị chuẩn
            List<Product> variants = productRepository.findByNameIgnoreCaseAndActiveTrue(p.getName());
            Product displayProd = variants.stream()
                    .min(java.util.Comparator.comparing(Product::getPrice))
                    .orElse(p);

            boolean available = branchId == null || availabilityMap.getOrDefault(displayProd.getId(), true);
            items.add(FavoriteDto.FavoriteItem.builder()
                    .id(fav.getId())
                    .productId(displayProd.getId())
                    .productName(displayProd.getName())
                    .categoryName(displayProd.getCategory() != null ? displayProd.getCategory().getName() : null)
                    .size(displayProd.getSize())
                    .price(displayProd.getPrice())
                    .imageUrl(displayProd.getImageUrl())
                    .createdAt(fav.getCreatedAt())
                    .available(available)
                    .build());
        }

        return FavoriteDto.FavoriteListResponse.builder()
                .items(items)
                .page(favoritePage.getNumber())
                .size(favoritePage.getSize())
                .totalElements(items.size())
                .totalPages(favoritePage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long productId, CurrentActor actor) {
        if (actor == null || !actor.isAuthenticated()
                || actor.getAccountId() == null
                || !actor.hasRole(Role.CUSTOMER)) {
            return false;
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return false;
        return productFavoriteRepository.existsByAccountIdAndProductName(actor.getAccountId(), product.getName());
    }

    @Transactional(readOnly = true)
    public long countFavorites(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return 0L;
        return productFavoriteRepository.countDistinctAccountsByProductName(product.getName());
    }

    private void validateAuthenticatedCustomer(CurrentActor actor) {
    if (actor == null || !actor.isAuthenticated()
            || actor.getAccountId() == null) {
        throw BusinessException.unauthorized(
                ErrorCode.ACCESS_DENIED,
                "Vui lòng đăng nhập để sử dụng tính năng danh sách yêu thích"
        );
    }

    if (!actor.hasRole(Role.CUSTOMER)) {
        throw BusinessException.forbidden(
                ErrorCode.ACCESS_DENIED,
                "Chức năng yêu thích chỉ dành cho khách hàng"
        );
    }
}
}
