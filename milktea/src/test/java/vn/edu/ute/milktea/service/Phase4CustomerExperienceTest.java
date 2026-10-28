package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.CatalogDto;
import vn.edu.ute.milktea.dto.FavoriteDto;
import vn.edu.ute.milktea.dto.ReviewDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.branch.BranchProduct;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.catalog.ProductFavorite;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderItem;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.review.ProductReview;
import vn.edu.ute.milktea.entity.review.ReviewMediaType;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductFavoriteRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.review.ProductReviewRepository;
import vn.edu.ute.milktea.repository.review.ReviewMediaRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.catalog.CatalogService;
import vn.edu.ute.milktea.service.catalog.ProductFavoriteService;
import vn.edu.ute.milktea.service.review.ProductReviewService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử toàn diện Giai đoạn 4: Trải nghiệm khách hàng
 * - CUS-01: Tìm kiếm & Lọc phân trang backend
 * - CUS-02: Bốn danh sách nổi bật (Top 20)
 * - CUS-03: Chi tiết món, size, topping & lịch sử
 * - CUS-05: Yêu thích (Wishlist) theo tài khoản
 * - CUS-06: Đánh giá, Bình luận & Media có kiểm duyệt
 */
@ExtendWith(MockitoExtension.class)
class Phase4CustomerExperienceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BranchProductRepository branchProductRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private ProductFavoriteRepository productFavoriteRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReviewMediaRepository reviewMediaRepository;

    private CatalogService catalogService;
    private ProductFavoriteService productFavoriteService;
    private ProductReviewService productReviewService;

    private Branch branch1;
    private Branch branch2;
    private Category teaCategory;
    private Category toppingCategory;
    private Product productOolong;
    private Product productBoba;
    private Account customer;
    private Account managerBranch1;
    private Account admin;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(
                categoryRepository,
                productRepository,
                branchProductRepository,
                branchRepository,
                productReviewRepository,
                productFavoriteRepository,
                orderItemRepository
        );

        productFavoriteService = new ProductFavoriteService(
                productFavoriteRepository,
                productRepository,
                accountRepository,
                branchProductRepository
        );

        productReviewService = new ProductReviewService(
                productReviewRepository,
                reviewMediaRepository,
                orderItemRepository,
                accountRepository,
                invoiceRepository,
                paymentRepository
        );

        branch1 = Branch.builder().id(1L).code("CN01").name("Chi nhánh Quận 1").build();
        branch2 = Branch.builder().id(2L).code("CN02").name("Chi nhánh Thủ Đức").build();

        teaCategory = Category.builder().id(10L).name("Trà Sữa").active(true).allowTopping(true).build();
        toppingCategory = Category.builder().id(20L).name("Topping").active(true).allowTopping(false).build();

        productOolong = Product.builder()
                .id(100L)
                .name("Trà Sữa O Long")
                .size("M")
                .price(new BigDecimal("35000"))
                .active(true)
                .category(teaCategory)
                .build();

        productBoba = Product.builder()
                .id(200L)
                .name("Trân Châu Đen")
                .size("M")
                .price(new BigDecimal("5000"))
                .active(true)
                .category(toppingCategory)
                .build();

        customer = Account.builder()
                .id(50L)
                .email("customer@milktea.vn")
                .fullName("Nguyễn Khách Hàng")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        managerBranch1 = Account.builder()
                .id(20L)
                .email("manager1@milktea.vn")
                .fullName("Quản Lý CN01")
                .role(Role.MANAGER)
                .active(true)
                .build();

        admin = Account.builder()
                .id(1L)
                .email("admin@milktea.vn")
                .fullName("Tổng Quản Trị")
                .role(Role.ADMIN)
                .active(true)
                .build();
    }

    // =========================================================================
    // 1. CUS-01: Menu, Tìm kiếm & Lọc phân trang backend
    // =========================================================================

    @Test
    @DisplayName("CUS-01: Tìm kiếm sản phẩm phân trang backend và giới hạn tối đa 50 món")
    void testSearchProducts_PaginationAndCaps() {
        Page<Product> pagedResult = new PageImpl<>(List.of(productOolong), PageRequest.of(0, 10), 1);
        when(productRepository.searchProducts(eq("O Long"), eq(10L), eq(new BigDecimal("20000")), eq(new BigDecimal("50000")), any(Pageable.class)))
                .thenReturn(pagedResult);

        CatalogDto.ProductSearchResponse res = catalogService.searchProducts(
                "O Long", 10L, new BigDecimal("20000"), new BigDecimal("50000"), "price_asc", 1L, 0, 100);

        assertNotNull(res);
        assertEquals(1, res.getItems().size());
        assertEquals("Trà Sữa O Long", res.getItems().get(0).getName());
        // Verify page size is capped at 50 max
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).searchProducts(any(), any(), any(), any(), pageableCaptor.capture());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("CUS-01: Tìm kiếm ánh xạ đúng trạng thái khả dụng tại chi nhánh")
    void testSearchProducts_BranchAvailabilityMapping() {
        Page<Product> pagedResult = new PageImpl<>(List.of(productOolong));
        when(productRepository.searchProducts(any(), any(), any(), any(), any())).thenReturn(pagedResult);

        // Branch 1 has productOolong marked as temporarily unavailable
        BranchProduct bp = BranchProduct.builder()
                .branch(branch1)
                .product(productOolong)
                .available(false)
                .build();
        when(branchProductRepository.findByBranchId(1L)).thenReturn(List.of(bp));

        CatalogDto.ProductSearchResponse res = catalogService.searchProducts(
                null, null, null, null, "newest", 1L, 0, 10);

        assertNotNull(res);
        assertEquals(1, res.getItems().size());
        assertFalse(res.getItems().get(0).isAvailable(), "Món phải thể hiện trạng thái tạm hết tại CN01");
    }

    // =========================================================================
    // 2. CUS-03: Chi tiết món uống, size, topping & trạng thái yêu thích
    // =========================================================================

    @Test
    @DisplayName("CUS-03: Lấy chi tiết món uống kèm danh sách size, topping và điểm đánh giá")
    void testGetProductDetail_Success() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(productOolong));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(productReviewRepository.findAverageRatingByProductId(100L)).thenReturn(4.8);
        when(productReviewRepository.countByProductIdAndHiddenFalse(100L)).thenReturn(15L);

        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");
        when(productFavoriteRepository.existsByAccountIdAndProductId(50L, 100L)).thenReturn(true);

        Product oolongSizeL = Product.builder().id(101L).name("Trà Sữa O Long").size("L").active(true).build();
        when(productRepository.findByNameIgnoreCaseAndActiveTrue("Trà Sữa O Long")).thenReturn(List.of(productOolong, oolongSizeL));
        when(productRepository.findToppingProducts()).thenReturn(List.of(productBoba));

        CatalogDto.ProductDetailResponse detail = catalogService.getProductDetail(100L, 1L, customerActor);

        assertNotNull(detail);
        assertEquals("Trà Sữa O Long", detail.getName());
        assertEquals("Chi nhánh Quận 1", detail.getBranchName());
        assertEquals(4.8, detail.getAverageRating());
        assertEquals(15L, detail.getReviewCount());
        assertTrue(detail.isFavorite());
        assertTrue(detail.getAvailableSizes().containsAll(List.of("L", "M")));
        assertEquals(1, detail.getToppings().size());
        assertEquals("Trân Châu Đen", detail.getToppings().get(0).getName());
    }

    @Test
    @DisplayName("CUS-03: Lấy chi tiết món không tồn tại hoặc đã ngừng bán ném ngoại lệ 404")
    void testGetProductDetail_NotFound() {
        Product inactiveProduct = Product.builder().id(999L).name("Món Cũ").active(false).build();
        when(productRepository.findById(999L)).thenReturn(Optional.of(inactiveProduct));

        assertThrows(BusinessException.class, () -> catalogService.getProductDetail(999L, 1L, null));
    }

    // =========================================================================
    // 3. CUS-05: Sản phẩm yêu thích (Wishlist)
    // =========================================================================

    @Test
    @DisplayName("CUS-05: Khách hàng thêm món vào yêu thích khi chưa có")
    void testToggleFavorite_Add() {
        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");
        when(productRepository.findById(100L)).thenReturn(Optional.of(productOolong));
        when(productFavoriteRepository.findByAccountIdAndProductId(50L, 100L)).thenReturn(Optional.empty());
        when(accountRepository.findById(50L)).thenReturn(Optional.of(customer));
        when(productFavoriteRepository.countByProductId(100L)).thenReturn(1L);

        FavoriteDto.ToggleFavoriteResponse res = productFavoriteService.toggleFavorite(100L, customerActor);

        assertTrue(res.isFavorite());
        assertEquals(1L, res.getTotalFavorites());
        verify(productFavoriteRepository).save(any(ProductFavorite.class));
    }

    @Test
    @DisplayName("CUS-05: Khách hàng nhấn lại để xóa món khỏi yêu thích")
    void testToggleFavorite_Remove() {
        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");
        when(productRepository.findById(100L)).thenReturn(Optional.of(productOolong));

        ProductFavorite existingFav = ProductFavorite.builder().id(1L).account(customer).product(productOolong).build();
        when(productFavoriteRepository.findByAccountIdAndProductId(50L, 100L)).thenReturn(Optional.of(existingFav));
        when(productFavoriteRepository.countByProductId(100L)).thenReturn(0L);

        FavoriteDto.ToggleFavoriteResponse res = productFavoriteService.toggleFavorite(100L, customerActor);

        assertFalse(res.isFavorite());
        assertEquals(0L, res.getTotalFavorites());
        verify(productFavoriteRepository).delete(existingFav);
    }

    @Test
    @DisplayName("CUS-05: Khách vãng lai chưa đăng nhập không được thao tác wishlist")
    void testToggleFavorite_Unauthenticated() {
        assertThrows(BusinessException.class, () -> productFavoriteService.toggleFavorite(100L, null));
        CurrentActor guestActor = CurrentActor.guest();
        assertThrows(BusinessException.class, () -> productFavoriteService.toggleFavorite(100L, guestActor));
    }

    // =========================================================================
    // 4. CUS-06: Đánh giá, Bình luận & Media có kiểm duyệt
    // =========================================================================

    @Test
    @DisplayName("CUS-06: Khách hàng gửi đánh giá hợp lệ khi đơn HOAN_THANH và đã thanh toán")
    void testCreateReview_Success() {
        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");

        Order order = Order.builder()
                .id(888L)
                .account(customer)
                .branch(branch1)
                .status(OrderStatus.HOAN_THANH)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(777L)
                .order(order)
                .product(productOolong)
                .quantity(2)
                .build();

        Invoice invoice = Invoice.builder().id(999L).order(order).build();

        when(orderItemRepository.findById(777L)).thenReturn(Optional.of(orderItem));
        when(productReviewRepository.existsByOrderItemId(777L)).thenReturn(false);
        when(invoiceRepository.findByOrderId(888L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.existsByInvoiceId(999L)).thenReturn(true);
        when(accountRepository.findById(50L)).thenReturn(Optional.of(customer));
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        String validContent = "Trà sữa vị ô long rất đậm đà, thơm mùi trà tự nhiên, độ ngọt vừa phải 50% đường rất ưng ý!";
        ReviewDto.CreateReviewRequest req = ReviewDto.CreateReviewRequest.builder()
                .orderItemId(777L)
                .rating(5)
                .content(validContent)
                .mediaList(List.of(ReviewDto.MediaItemRequest.builder().mediaUrl("https://cloudinary.com/tea.jpg").mediaType(ReviewMediaType.IMAGE).build()))
                .build();

        ReviewDto.ReviewResponse res = productReviewService.createReview(req, customerActor);

        assertNotNull(res);
        assertEquals(5, res.getRating());
        assertEquals(validContent, res.getContent());
        assertFalse(res.getHidden());
        assertEquals("Chi nhánh Quận 1", res.getBranchName());
        assertEquals(1, res.getMediaList().size());
        verify(productReviewRepository).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("CUS-06: Chặn đánh giá nếu nội dung sau trim dưới 50 ký tự")
    void testCreateReview_ShortContent_ThrowsBadRequest() {
        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");
        ReviewDto.CreateReviewRequest req = ReviewDto.CreateReviewRequest.builder()
                .orderItemId(777L)
                .rating(5)
                .content("Ngon quá bạn ơi!") // Dưới 50 ký tự
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> productReviewService.createReview(req, customerActor));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("CUS-06: Chặn đánh giá trùng lặp trên cùng một dòng chi tiết đơn hàng")
    void testCreateReview_DuplicateOrderItem_ThrowsConflict() {
        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");
        OrderItem orderItem = OrderItem.builder().id(777L).build();
        when(orderItemRepository.findById(777L)).thenReturn(Optional.of(orderItem));
        when(productReviewRepository.existsByOrderItemId(777L)).thenReturn(true);

        String longContent = "Trà sữa đậm đà rất ngon, trân châu dai giòn sần sật, dịch vụ tuyệt vời sẽ ủng hộ lần sau!";
        ReviewDto.CreateReviewRequest req = ReviewDto.CreateReviewRequest.builder()
                .orderItemId(777L)
                .rating(5)
                .content(longContent)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> productReviewService.createReview(req, customerActor));
        assertEquals(ErrorCode.IDEMPOTENCY_CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("CUS-06: Chặn đánh giá nếu đơn hàng chưa hoàn thành phục vụ hoặc chưa thanh toán")
    void testCreateReview_OrderNotCompletedOrUnpaid() {
        CurrentActor customerActor = CurrentActor.customer(50L, "customer@milktea.vn");
        Order uncompletedOrder = Order.builder().id(888L).account(customer).status(OrderStatus.DANG_CHE_BIEN).build();
        OrderItem orderItem = OrderItem.builder().id(777L).order(uncompletedOrder).product(productOolong).build();

        when(orderItemRepository.findById(777L)).thenReturn(Optional.of(orderItem));
        when(productReviewRepository.existsByOrderItemId(777L)).thenReturn(false);

        String longContent = "Trà sữa đậm đà rất ngon, trân châu dai giòn sần sật, dịch vụ tuyệt vời sẽ ủng hộ lần sau!";
        ReviewDto.CreateReviewRequest req = ReviewDto.CreateReviewRequest.builder()
                .orderItemId(777L)
                .rating(5)
                .content(longContent)
                .build();

        assertThrows(BusinessException.class, () -> productReviewService.createReview(req, customerActor));
    }

    @Test
    @DisplayName("CUS-06: Quản lý chi nhánh chỉ được kiểm duyệt đánh giá thuộc chi nhánh mình")
    void testModerateReview_BranchScopeEnforced() {
        ProductReview reviewBranch1 = ProductReview.builder()
                .id(1000L)
                .branch(branch1)
                .rating(1)
                .content("Bình luận cần ẩn...")
                .hidden(false)
                .build();

        when(productReviewRepository.findById(1000L)).thenReturn(Optional.of(reviewBranch1));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(managerBranch1));
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        // Manager of Branch 1 moderating Branch 1 -> Allowed
        CurrentActor managerActorBranch1 = CurrentActor.authenticated(20L, Role.MANAGER, 1L, List.of(1L));
        ReviewDto.ModerateReviewRequest hideReq = ReviewDto.ModerateReviewRequest.builder()
                .hidden(true)
                .reason("Nội dung chứa từ ngữ xúc phạm")
                .build();

        ReviewDto.ReviewResponse res = productReviewService.moderateReview(1000L, hideReq, managerActorBranch1);
        assertTrue(res.getHidden());
        assertEquals("Nội dung chứa từ ngữ xúc phạm", res.getHiddenReason());

        // Manager of Branch 2 attempting to moderate Branch 1 -> Forbidden
        CurrentActor managerActorBranch2 = CurrentActor.authenticated(22L, Role.MANAGER, 2L, List.of(2L));
        assertThrows(BusinessException.class, () -> productReviewService.moderateReview(1000L, hideReq, managerActorBranch2));
    }

    @Test
    @DisplayName("CUS-06: Ẩn đánh giá bắt buộc phải có lý do (reason)")
    void testModerateReview_RequireReasonWhenHiding() {
        ProductReview review = ProductReview.builder().id(1000L).branch(branch1).build();
        when(productReviewRepository.findById(1000L)).thenReturn(Optional.of(review));

        CurrentActor adminActor = CurrentActor.authenticated(1L, Role.ADMIN);
        ReviewDto.ModerateReviewRequest invalidReq = ReviewDto.ModerateReviewRequest.builder()
                .hidden(true)
                .reason("  ") // Trống
                .build();

        assertThrows(BusinessException.class, () -> productReviewService.moderateReview(1000L, invalidReq, adminActor));
    }

    // =========================================================================
    // 5. CUS-02: Bốn danh sách nổi bật (Top 20)
    // =========================================================================

    @Test
    @DisplayName("CUS-02: Lấy danh sách Mới nhất (newest) giới hạn tối đa 20 sản phẩm")
    void testGetFeaturedProducts_Newest() {
        when(productRepository.findByActiveTrueOrderByIdDesc(any(Pageable.class)))
                .thenReturn(List.of(productOolong, productBoba));

        CatalogDto.FeaturedProductsResponse res = catalogService.getFeaturedProducts("newest", 1L, 25);

        assertNotNull(res);
        assertEquals("newest", res.getType());
        assertEquals(2, res.getProducts().size());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findByActiveTrueOrderByIdDesc(pageableCaptor.capture());
        assertEquals(20, pageableCaptor.getValue().getPageSize(), "Giới hạn phải được chặn ở mức tối đa 20 sản phẩm");
    }

    @Test
    @DisplayName("CUS-02: Bán chạy nhất (best_seller) tính theo đơn HOAN_THANH và đã thanh toán tại chi nhánh")
    void testGetFeaturedProducts_BestSeller() {
        when(orderItemRepository.findBestSellerProductIds(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(100L));
        when(productRepository.findByIdInAndActiveTrue(List.of(100L)))
                .thenReturn(List.of(productOolong));

        CatalogDto.FeaturedProductsResponse res = catalogService.getFeaturedProducts("best_seller", 1L, 10);

        assertNotNull(res);
        assertEquals("best_seller", res.getType());
        assertEquals(1, res.getProducts().size());
        assertEquals(100L, res.getProducts().get(0).getId());
        verify(orderItemRepository).findBestSellerProductIds(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("CUS-02: Đánh giá cao nhất (top_rated) sắp xếp theo điểm sao trung bình các đánh giá hiển thị")
    void testGetFeaturedProducts_TopRated() {
        when(productReviewRepository.findTopRatedProductIds(any(Pageable.class)))
                .thenReturn(List.of(100L));
        when(productRepository.findByIdInAndActiveTrue(List.of(100L)))
                .thenReturn(List.of(productOolong));

        CatalogDto.FeaturedProductsResponse res = catalogService.getFeaturedProducts("top_rated", 1L, 10);

        assertNotNull(res);
        assertEquals("top_rated", res.getType());
        assertEquals(1, res.getProducts().size());
        verify(productReviewRepository).findTopRatedProductIds(any(Pageable.class));
    }

    @Test
    @DisplayName("CUS-02: Yêu thích nhiều nhất (most_favorited) sắp xếp theo số lượt khách lưu wishlist")
    void testGetFeaturedProducts_MostFavorited() {
        when(productFavoriteRepository.findMostFavoritedProductIds(any(Pageable.class)))
                .thenReturn(List.of(100L));
        when(productRepository.findByIdInAndActiveTrue(List.of(100L)))
                .thenReturn(List.of(productOolong));

        CatalogDto.FeaturedProductsResponse res = catalogService.getFeaturedProducts("most_favorited", 1L, 10);

        assertNotNull(res);
        assertEquals("most_favorited", res.getType());
        assertEquals(1, res.getProducts().size());
        verify(productFavoriteRepository).findMostFavoritedProductIds(any(Pageable.class));
    }
}
