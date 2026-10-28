package vn.edu.ute.milktea.service.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.CatalogDto;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductFavoriteRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.review.ProductReviewRepository;
import vn.edu.ute.milktea.security.CurrentActor;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service quản lý danh mục, menu, tìm kiếm, lọc, chi tiết món và danh sách nổi bật (CUS-01, CUS-02, CUS-03).
 */
@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final BranchRepository branchRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final OrderItemRepository orderItemRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public CatalogService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            BranchProductRepository branchProductRepository,
            BranchRepository branchRepository,
            ProductReviewRepository productReviewRepository,
            ProductFavoriteRepository productFavoriteRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.branchProductRepository = branchProductRepository;
        this.branchRepository = branchRepository;
        this.productReviewRepository = productReviewRepository;
        this.productFavoriteRepository = productFavoriteRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public CatalogService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            BranchProductRepository branchProductRepository
    ) {
        this(categoryRepository, productRepository, branchProductRepository, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public CatalogDto.MenuResponse getMenu(Long branchId) {
        var categories = categoryRepository.findAllByActiveTrue();
        var allProducts = productRepository.findAllByActiveTrue();

        Map<Long, Boolean> availabilityMap = getAvailabilityMap(branchId);

        List<CatalogDto.CategoryItem> categoryItems = categories.stream().map(cat -> {
            var products = allProducts.stream()
                    .filter(p -> p.getCategory().getId().equals(cat.getId()))
                    .map(p -> mapProduct(p, availabilityMap.getOrDefault(p.getId(), true)))
                    .toList();

            return CatalogDto.CategoryItem.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .active(cat.getActive())
                    .allowTopping(cat.getAllowTopping())
                    .products(products)
                    .build();
        }).toList();

        return CatalogDto.MenuResponse.builder()
                .categories(categoryItems)
                .build();
    }

    @Transactional(readOnly = true)
    public CatalogDto.MenuResponse getMenu() {
        return getMenu(null);
    }

    @Transactional(readOnly = true)
    public CatalogDto.ProductItem getProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));
        return mapProduct(p);
    }

    @Transactional(readOnly = true)
    public List<CatalogDto.CategoryItem> getCategories() {
        return categoryRepository.findAllByActiveTrue().stream().map(cat ->
                CatalogDto.CategoryItem.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .active(cat.getActive())
                        .allowTopping(cat.getAllowTopping())
                        .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDto.ProductItem> getProducts() {
        return productRepository.findAllByActiveTrue().stream().map(this::mapProduct).toList();
    }

    /**
     * Tìm kiếm và lọc sản phẩm phân trang chuẩn backend (CUS-01).
     */
    @Transactional(readOnly = true)
    public CatalogDto.ProductSearchResponse searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            Long branchId,
            int page,
            int size
    ) {
        int limitSize = Math.min(Math.max(size, 1), 50);
        int pageIndex = Math.max(page, 0);

        Sort sort = switch (sortBy != null ? sortBy.trim().toLowerCase() : "") {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "name_asc" -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("id"));
        };

        Pageable pageable = PageRequest.of(pageIndex, limitSize, sort);
        Page<Product> pPage = productRepository.searchProducts(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                categoryId,
                minPrice,
                maxPrice,
                pageable
        );

        Map<Long, Boolean> availabilityMap = getAvailabilityMap(branchId);

        List<CatalogDto.ProductItem> items = pPage.getContent().stream()
                .map(p -> mapProduct(p, isProductAvailable(p.getId(), branchId, availabilityMap)))
                .toList();

        return CatalogDto.ProductSearchResponse.builder()
                .items(items)
                .page(pPage.getNumber())
                .size(pPage.getSize())
                .totalElements(pPage.getTotalElements())
                .totalPages(pPage.getTotalPages())
                .build();
    }

    /**
     * Chi tiết món uống, size, topping, điểm đánh giá và trạng thái yêu thích (CUS-03).
     */
    @Transactional(readOnly = true)
    public CatalogDto.ProductDetailResponse getProductDetail(Long productId, Long branchId, CurrentActor actor) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));

        if (!Boolean.TRUE.equals(p.getActive())) {
            throw BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Món uống hiện không hoạt động");
        }

        boolean available = true;
        String branchName = null;
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId).orElse(null);
            if (branch != null) {
                branchName = branch.getName();
            }
            var bpOpt = branchProductRepository.findByBranchIdAndProductId(branchId, productId);
            if (bpOpt.isPresent()) {
                available = Boolean.TRUE.equals(bpOpt.get().getActive()) && Boolean.TRUE.equals(bpOpt.get().getAvailable());
            } else {
                available = branchProductRepository.findByBranchId(branchId).isEmpty();
            }
        }

        Double avgRating = productReviewRepository.findAverageRatingByProductId(productId);
        long reviewCount = productReviewRepository.countByProductIdAndHiddenFalse(productId);

        boolean isFavorite = false;
        if (actor != null && actor.isAuthenticated() && actor.getAccountId() != null) {
            isFavorite = productFavoriteRepository.existsByAccountIdAndProductName(actor.getAccountId(), p.getName());
            if (!isFavorite) {
                isFavorite = productFavoriteRepository.existsByAccountIdAndProductId(actor.getAccountId(), productId);
            }
        }

        List<Product> sameNameProducts = productRepository.findByNameIgnoreCaseAndActiveTrue(p.getName());
        List<String> sizes = sameNameProducts.stream()
                .map(Product::getSize)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Boolean> productAvailMap = getAvailabilityMap(branchId);
        List<CatalogDto.SizeOption> sizeOptions = (sameNameProducts != null)
                ? sameNameProducts.stream()
                        .sorted(Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                        .map(v -> CatalogDto.SizeOption.builder()
                                .id(v.getId())
                                .size(v.getSize())
                                .price(v.getPrice())
                                .available(isProductAvailable(v.getId(), branchId, productAvailMap))
                                .build())
                        .toList()
                : Collections.emptyList();

        List<CatalogDto.ProductItem> toppings = new ArrayList<>();
        if (p.getCategory() != null && Boolean.TRUE.equals(p.getCategory().getAllowTopping())) {
            List<Product> toppingProds = productRepository.findByCategoryAllowToppingFalseAndActiveTrue();
            Map<Long, Boolean> toppingAvailMap = getAvailabilityMap(branchId);
            toppings = toppingProds.stream()
                    .map(tp -> mapProduct(tp, isProductAvailable(tp.getId(), branchId, toppingAvailMap)))
                    .toList();
        }

        return CatalogDto.ProductDetailResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .allowTopping(p.getCategory() != null && Boolean.TRUE.equals(p.getCategory().getAllowTopping()))
                .name(p.getName())
                .size(p.getSize())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .active(p.getActive())
                .available(available)
                .branchId(branchId)
                .branchName(branchName)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .isFavorite(isFavorite)
                .availableSizes(sizes)
                .sizeOptions(sizeOptions)
                .toppings(toppings)
                .build();
    }

    /**
     * Danh sách nổi bật: Bán chạy nhất, Đánh giá cao nhất, Yêu thích nhất (CUS-02 - Top 20).
     */
    @Transactional(readOnly = true)
    public CatalogDto.FeaturedProductsResponse getFeaturedProducts(String type, Long branchId, int limit) {
        int limitCount = Math.min(Math.max(limit, 1), 20);
        String featType = (type != null && !type.isBlank()) ? type.toLowerCase().trim().replace("-", "_") : "best_seller";

        List<Product> orderedProducts = new ArrayList<>();
        Pageable topPage = PageRequest.of(0, limitCount);

        switch (featType) {
            case "top_rated" -> {
                List<Long> topRatedIds = productReviewRepository.findTopRatedProductIds(topPage);
                orderedProducts.addAll(loadProductsInOrder(topRatedIds));
            }
            case "most_favorited" -> {
                List<String> favNames = productFavoriteRepository.findMostFavoritedProductNames(topPage);
                if (favNames != null && !favNames.isEmpty()) {
                    Set<String> seenNames = new HashSet<>();
                    for (String name : favNames) {
                        if (name == null || seenNames.contains(name.toLowerCase())) continue;
                        seenNames.add(name.toLowerCase());
                        List<Product> variants = productRepository.findByNameIgnoreCaseAndActiveTrue(name);
                        if (variants != null && !variants.isEmpty()) {
                            Product smallest = variants.stream()
                                    .min(Comparator.comparing(Product::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                                    .orElse(variants.get(0));
                            orderedProducts.add(smallest);
                        }
                    }
                } else {
                    List<Long> favIds = productFavoriteRepository.findMostFavoritedProductIds(topPage);
                    if (favIds != null && !favIds.isEmpty()) {
                        orderedProducts.addAll(loadProductsInOrder(favIds));
                    }
                }
            }
            case "newest" -> {
                List<Product> newest = productRepository.findNewestProducts(topPage);
                if (newest != null && !newest.isEmpty()) {
                    orderedProducts.addAll(newest);
                } else {
                    orderedProducts.addAll(productRepository.findByActiveTrueOrderByIdDesc(topPage));
                }
            }
            case "best_seller" -> {
                List<Long> bestSellerIds = orderItemRepository.findBestSellerProductIds(branchId, topPage);
                orderedProducts.addAll(loadProductsInOrder(bestSellerIds));
            }
            default -> {
                featType = "best_seller";
                List<Long> bestSellerIds = orderItemRepository.findBestSellerProductIds(branchId, topPage);
                orderedProducts.addAll(loadProductsInOrder(bestSellerIds));
            }
        }

        Map<Long, Boolean> availabilityMap = getAvailabilityMap(branchId);
        List<CatalogDto.ProductItem> productItems = orderedProducts.stream()
                .map(p -> mapProduct(p, isProductAvailable(p.getId(), branchId, availabilityMap)))
                .toList();

        String branchName = null;
        if (branchId != null) {
            Branch b = branchRepository != null ? branchRepository.findById(branchId).orElse(null) : null;
            if (b != null) branchName = b.getName();
        }

        return CatalogDto.FeaturedProductsResponse.builder()
                .type(featType)
                .branchId(branchId)
                .branchName(branchName)
                .products(productItems)
                .build();
    }

    private List<Product> loadProductsInOrder(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Product> fetched = productRepository.findByIdInAndActiveTrue(productIds);
        Map<Long, Product> map = fetched.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Product> result = new ArrayList<>();
        for (Long id : productIds) {
            Product p = map.get(id);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    private Map<Long, Boolean> getAvailabilityMap(Long branchId) {
        Map<Long, Boolean> availabilityMap = new HashMap<>();
        if (branchId != null) {
            if (branchRepository != null) {
                var bOpt = branchRepository.findById(branchId);
                if (bOpt.isPresent() && (Boolean.TRUE.equals(bOpt.get().getIsCentral()) || "KHOTONG".equalsIgnoreCase(bOpt.get().getIdentifier()))) {
                    return availabilityMap;
                }
            }
            branchProductRepository.findByBranchId(branchId).forEach(bp -> {
                if (bp.getProduct() != null) {
                    boolean sellable = Boolean.TRUE.equals(bp.getActive()) && Boolean.TRUE.equals(bp.getAvailable());
                    availabilityMap.put(bp.getProduct().getId(), sellable);
                }
            });
        }
        return availabilityMap;
    }

    private boolean isProductAvailable(Long productId, Long branchId, Map<Long, Boolean> availabilityMap) {
        if (branchId == null || availabilityMap == null) return true;
        if (availabilityMap.containsKey(productId)) {
            return Boolean.TRUE.equals(availabilityMap.get(productId));
        }
        return availabilityMap.isEmpty();
    }

    private CatalogDto.ProductItem mapProduct(Product p) {
        return mapProduct(p, true);
    }

    private CatalogDto.ProductItem mapProduct(Product p, boolean available) {
        return CatalogDto.ProductItem.builder()
                .id(p.getId())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .name(p.getName())
                .size(p.getSize())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .active(p.getActive())
                .available(available)
                .build();
    }
}
