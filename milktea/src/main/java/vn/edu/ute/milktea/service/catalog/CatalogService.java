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
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
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
                available = false;
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
            List<Product> toppingProds = productRepository.findToppingProducts();
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
        return false;
    }

    @Transactional(readOnly = true)
    public CatalogDto.MenuPageResponse getPaginatedMenu(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String featuredType,
            Long branchId,
            int page,
            int size
    ) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        int pageIndex = Math.max(page, 1);

        List<Product> allActive = productRepository.findAllByActiveTrue();
        Map<Long, Boolean> availabilityMap = getAvailabilityMap(branchId);

        // 1. Lọc sản phẩm theo sellability predicate chung
        List<Product> sellable = allActive.stream()
                .filter(p -> p.getCategory() != null && Boolean.TRUE.equals(p.getCategory().getActive()))
                .filter(p -> isProductAvailable(p.getId(), branchId, availabilityMap))
                .toList();

        // 2. Gom nhóm theo tên món (distinct drinks), chọn biến thể đại diện có giá thấp nhất
        Map<String, Product> smallestVariants = new LinkedHashMap<>();
        for (Product p : sellable) {
            String norm = (p.getName() != null) ? p.getName().trim().toLowerCase() : "";
            if (!smallestVariants.containsKey(norm) || p.getPrice().compareTo(smallestVariants.get(norm).getPrice()) < 0
                    || (p.getPrice().compareTo(smallestVariants.get(norm).getPrice()) == 0 && p.getId() < smallestVariants.get(norm).getId())) {
                smallestVariants.put(norm, p);
            }
        }
        List<Product> distinctDrinks = new ArrayList<>(smallestVariants.values());

        // 3. Áp dụng bộ sưu tập nổi bật (Top 20) nếu được chọn
        String fType = (featuredType != null && !featuredType.isBlank()) ? featuredType.toLowerCase().trim().replace("-", "_") : "all";
        boolean isExplicitFeaturedTab = !"all".equals(fType);
        if ("all".equals(fType) && sortBy != null) {
            String s = sortBy.trim().toLowerCase().replace("-", "_");
            if (List.of("best_seller", "top20", "top_20", "newest", "top_rated", "most_favorited").contains(s)) {
                fType = s;
            }
        }

        if (!"all".equals(fType)) {
            Pageable top20Page = PageRequest.of(0, 50);
            List<String> rankedNames = new ArrayList<>();
            switch (fType) {
                case "top_rated" -> {
                    List<Long> topRatedIds = productReviewRepository.findTopRatedProductIds(top20Page);
                    if (topRatedIds != null) {
                        for (Long id : topRatedIds) {
                            allActive.stream().filter(p -> p.getId().equals(id)).findFirst()
                                    .ifPresent(p -> rankedNames.add(p.getName().trim().toLowerCase()));
                        }
                    }
                }
                case "most_favorited" -> {
                    List<String> favNames = productFavoriteRepository.findMostFavoritedProductNames(top20Page);
                    if (favNames != null) {
                        for (String fn : favNames) {
                            if (fn != null) rankedNames.add(fn.trim().toLowerCase());
                        }
                    }
                }
                case "newest" -> {
                    List<Product> newest = productRepository.findNewestProducts(top20Page);
                    if (newest != null) {
                        for (Product p : newest) {
                            rankedNames.add(p.getName().trim().toLowerCase());
                        }
                    }
                }
                case "top20", "top_20", "best_seller" -> {
                    List<Long> bestSellerIds = orderItemRepository.findBestSellerProductIds(branchId, top20Page);
                    if (bestSellerIds != null) {
                        for (Long id : bestSellerIds) {
                            allActive.stream().filter(p -> p.getId().equals(id)).findFirst()
                                    .ifPresent(p -> rankedNames.add(p.getName().trim().toLowerCase()));
                        }
                    }
                    if (rankedNames.size() < 20) {
                        for (Product p : distinctDrinks) {
                            String norm = p.getName().trim().toLowerCase();
                            if (!rankedNames.contains(norm)) {
                                rankedNames.add(norm);
                            }
                            if (rankedNames.size() >= 20) break;
                        }
                    }
                }
            }

            Map<String, Integer> rankMap = new LinkedHashMap<>();
            int r = 0;
            for (String rn : rankedNames) {
                if (!rankMap.containsKey(rn)) {
                    rankMap.put(rn, r++);
                }
            }

            if (isExplicitFeaturedTab) {
                distinctDrinks = distinctDrinks.stream()
                        .filter(p -> rankMap.containsKey(p.getName().trim().toLowerCase()))
                        .sorted(Comparator.comparingInt(p -> rankMap.get(p.getName().trim().toLowerCase())))
                        .limit(20)
                        .toList();
            } else {
                distinctDrinks = distinctDrinks.stream()
                        .sorted(Comparator.comparingInt((Product p) -> rankMap.getOrDefault(p.getName().trim().toLowerCase(), 999999))
                                .thenComparing(Product::getId))
                        .toList();
            }
        }

        // 4. Lọc theo từ khóa, danh mục, khoảng giá
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;
        String noAccentKw = (kw != null) ? removeDiacritics(kw) : null;
        List<Product> filtered = distinctDrinks.stream()
                .filter(p -> {
                    if (kw != null) {
                        String name = p.getName() != null ? p.getName().toLowerCase() : "";
                        String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                        String noAccentName = removeDiacritics(name);
                        String noAccentDesc = removeDiacritics(desc);
                        boolean matchesName = name.contains(kw) || noAccentName.contains(noAccentKw);
                        boolean matchesDesc = desc.contains(kw) || noAccentDesc.contains(noAccentKw);
                        if (!matchesName && !matchesDesc) return false;
                    }
                    if (categoryId != null && categoryId > 0) {
                        if (p.getCategory() == null || !categoryId.equals(p.getCategory().getId())) return false;
                    }
                    if (minPrice != null && p.getPrice().compareTo(minPrice) < 0) return false;
                    if (maxPrice != null && p.getPrice().compareTo(maxPrice) > 0) return false;
                    return true;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // 5. Sắp xếp với tie-break ổn định
        if ("all".equals(fType) || (sortBy != null && !"default".equalsIgnoreCase(sortBy))) {
            String sort = (sortBy != null) ? sortBy.trim().toLowerCase() : "";
            switch (sort) {
                case "price_asc" -> filtered.sort(Comparator.comparing(Product::getPrice).thenComparing(Product::getId));
                case "price_desc" -> filtered.sort(Comparator.comparing(Product::getPrice).reversed().thenComparing(Product::getId));
                case "name_asc" -> filtered.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Product::getId));
                default -> {
                    if ("all".equals(fType)) {
                        filtered.sort(Comparator.comparing(Product::getId));
                    }
                }
            }
        }

        // 6. Phân trang
        long totalItems = filtered.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        int effectivePage = Math.max(1, Math.min(pageIndex, Math.max(totalPages, 1)));

        int start = (effectivePage - 1) * pageSize;
        int end = Math.min(start + pageSize, (int) totalItems);

        List<Product> pageSlice = (start < totalItems) ? filtered.subList(start, end) : Collections.emptyList();

        List<CatalogDto.ProductItem> items = pageSlice.stream()
                .map(p -> mapProduct(p, isProductAvailable(p.getId(), branchId, availabilityMap)))
                .toList();

        return CatalogDto.MenuPageResponse.builder()
                .items(items)
                .page(effectivePage)
                .pageSize(pageSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .hasNext(effectivePage < totalPages)
                .hasPrevious(effectivePage > 1)
                .build();
    }

    private CatalogDto.ProductItem mapProduct(Product p) {
        return mapProduct(p, true);
    }

    private CatalogDto.ProductItem mapProduct(Product p, boolean available) {
        return CatalogDto.ProductItem.builder()
                .id(p.getId())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .allowTopping(p.getCategory() != null ? p.getCategory().getAllowTopping() : true)
                .name(p.getName())
                .size(p.getSize())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .active(p.getActive())
                .available(available)
                .build();
    }

    private String removeDiacritics(String str) {
        if (str == null) return "";
        String nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'D').toLowerCase();
    }
}
