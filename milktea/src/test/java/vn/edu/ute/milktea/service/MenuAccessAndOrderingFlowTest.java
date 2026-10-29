package vn.edu.ute.milktea.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.controller.CustomerPageController;
import vn.edu.ute.milktea.dto.BranchDto;
import vn.edu.ute.milktea.dto.CartDto;
import vn.edu.ute.milktea.dto.CatalogDto;
import vn.edu.ute.milktea.dto.OrderDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderSource;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.settings.GlobalSettings;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.branch.StaffBranchRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderIngredientSnapshotRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.order.OrderItemToppingRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentNoticeRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.review.ProductReviewRepository;
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.table.SessionCartRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.GuestAccessService;
import vn.edu.ute.milktea.service.branch.BranchService;
import vn.edu.ute.milktea.service.catalog.CatalogService;
import vn.edu.ute.milktea.service.common.IdempotencyService;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.service.order.OrderService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;
import vn.edu.ute.milktea.service.recipe.FixedRecipeService;
import vn.edu.ute.milktea.service.table.SessionCartService;
import vn.edu.ute.milktea.service.table.TableContextService;
import vn.edu.ute.milktea.service.table.TableSessionService;
import vn.edu.ute.milktea.service.voucher.VoucherService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MenuAccessAndOrderingFlowTest {

    // CustomerPageController mocks
    @Mock private DiningTableRepository tableRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GlobalSettingsRepository settingsRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CatalogService catalogService;
    @Mock private BranchService branchService;
    @Mock private ProductReviewRepository productReviewRepository;
    @Mock private TableSessionRepository sessionRepository;

    // OrderService mocks
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderItemToppingRepository toppingRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentNoticeRepository paymentNoticeRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private ProductRecipeRepository recipeRepository;
    @Mock private BusinessAuditRepository auditRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BranchProductRepository branchProductRepository;
    @Mock private CancellationRequestRepository cancellationRequestRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private OrderIngredientSnapshotRepository orderIngredientSnapshotRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private SessionCartRepository sessionCartRepository;
    @Mock private InventoryService inventoryService;
    @Mock private TableSessionService tableSessionService;
    @Mock private SessionCartService sessionCartService;
    @Mock private VoucherService voucherService;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;
    @Mock private GuestAccessService guestAccessService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private FixedRecipeService fixedRecipeService;
    @Mock private StaffBranchRepository staffBranchRepository;

    // TableContextService mock
    @Mock private TableContextService tableContextService;

    // HTTP mocks
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession httpSession;

    @InjectMocks
    private CustomerPageController customerPageController;
    @InjectMocks
    private OrderService orderService;

    private Branch branch1;
    private DiningTable table1;
    private GlobalSettings globalSettings;

    @BeforeEach
    void setUp() {
        branch1 = Branch.builder().id(1L).code("CN01").name("Chi nhánh Quận 1").address("123 Lê Lợi").status("HOAT_DONG").build();
        table1 = DiningTable.builder()
                .id(10L)
                .name("Bàn 01")
                .qrCode("QR_TABLE_10")
                .active(true)
                .status(TableStatus.TRONG)
                .branch(branch1)
                .build();
        globalSettings = GlobalSettings.builder()
                .id(1)
                .shopName("MilkTea House")
                .address("Trụ sở chính TP.HCM")
                .build();
    }

    // Ca 1: Truy cập / chưa đăng nhập -> trang giới thiệu
    @Test
    @DisplayName("Ca 1: Truy cập / chưa đăng nhập -> hiển thị trang giới thiệu thương hiệu (customer/home)")
    void testRootAccessAnonymousRendersHomePage() {
        when(settingsRepository.findById(1)).thenReturn(Optional.of(globalSettings));
        when(branchService.getActiveBranches()).thenReturn(List.of(BranchDto.BranchResponse.builder().id(1L).name("CN 1").build()));
        when(catalogService.getPaginatedMenu(isNull(), isNull(), isNull(), isNull(), eq("default"), eq("best_seller"), isNull(), eq(1), eq(8)))
                .thenReturn(CatalogDto.MenuPageResponse.builder().items(Collections.emptyList()).page(1).totalItems(0).totalPages(0).build());
        when(productReviewRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        Model model = new ExtendedModelMap();
        String view = customerPageController.homePage(null, request, response, model);

        assertEquals("customer/home", view);
        assertEquals(globalSettings, model.getAttribute("settings"));
        assertNotNull(model.getAttribute("branches"));
        assertEquals("home", model.getAttribute("activePage"));
    }

    // Ca 2: Truy cập / bằng tài khoản customer chưa quét QR -> trang giới thiệu
    @Test
    @DisplayName("Ca 2: Khách đã đăng nhập nhưng chưa quét QR truy cập / -> vẫn hiển thị trang giới thiệu, không tự cấp quyền gọi món")
    void testRootAccessLoggedInCustomerWithoutQrRendersHomePage() {
        when(settingsRepository.findById(1)).thenReturn(Optional.of(globalSettings));
        when(branchService.getActiveBranches()).thenReturn(Collections.emptyList());
        when(catalogService.getPaginatedMenu(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(CatalogDto.MenuPageResponse.builder().items(Collections.emptyList()).page(1).totalItems(0).totalPages(0).build());
        when(productReviewRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        Model model = new ExtendedModelMap();
        String view = customerPageController.homePage(null, request, response, model);

        assertEquals("customer/home", view);
        assertNull(model.getAttribute("currentTable"));
        assertTrue((Boolean) model.getAttribute("isViewOnly"));
    }

    // Ca 3: Bấm Menu -> xem món, chi tiết và giá theo chi nhánh; không có thao tác đặt món
    @Test
    @DisplayName("Ca 3: Truy cập /menu không có ngữ cảnh bàn -> chế độ chỉ xem isViewOnly=true, currentTable=null")
    void testMenuAccessWithoutTableRendersViewOnlyMenu() {
        when(settingsRepository.findById(1)).thenReturn(Optional.of(globalSettings));
        when(categoryRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());
        when(branchService.getActiveBranches()).thenReturn(List.of(BranchDto.BranchResponse.builder().id(1L).name("CN 1").build()));
        when(catalogService.getPaginatedMenu(isNull(), isNull(), isNull(), isNull(), eq("default"), eq("all"), isNull(), eq(1), eq(12)))
                .thenReturn(CatalogDto.MenuPageResponse.builder().items(Collections.emptyList()).page(1).totalItems(0).totalPages(0).build());

        Model model = new ExtendedModelMap();
        String view = customerPageController.menuPage(null, null, 1, 12, "default", "all", null, null, request, response, model);

        assertEquals("customer/menu", view);
        assertEquals(true, model.getAttribute("isViewOnly"));
        assertNull(model.getAttribute("currentTable"));
        assertEquals("menu", model.getAttribute("activePage"));
    }

    // Ca 4: Đăng nhập từ chế độ chỉ xem -> vẫn không được đặt món khi chưa có phiên QR hợp lệ
    @Test
    @DisplayName("Ca 4: Khách đã đăng nhập cố tình gọi API tạo đơn bàn không có token -> bị từ chối ACCESS_DENIED")
    void testLoggedInCustomerWithoutQrTokenCannotCreateTableOrder() {
        when(tableRepository.findByIdWithLock(10L)).thenReturn(Optional.of(table1));

        CurrentActor customerActor = CurrentActor.authenticated(200L, Role.CUSTOMER, null, Collections.emptyList());
        OrderDto.CreateTableOrderRequest req = OrderDto.CreateTableOrderRequest.builder()
                .tableId(10L)
                .tableToken(null) // Không có token QR
                .items(List.of(CartDto.CartItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createTableOrder(req, customerActor, "IDEMP_KEY_001")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Cần quét mã QR hợp lệ"));
    }

    // Ca 5: Gọi trực tiếp API đặt món khi chưa có quyền phiên -> bị từ chối, không phát sinh dữ liệu nghiệp vụ
    @Test
    @DisplayName("Ca 5: Anonymous gọi trực tiếp API đặt món không có quyền -> 403 ACCESS_DENIED, không tạo đơn")
    void testDirectApiCallWithoutQrTokenRejected() {
        when(tableRepository.findByIdWithLock(10L)).thenReturn(Optional.of(table1));

        OrderDto.CreateTableOrderRequest req = OrderDto.CreateTableOrderRequest.builder()
                .tableId(10L)
                .tableToken("INVALID_TOKEN_XYZ")
                .items(List.of(CartDto.CartItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        when(guestAccessService.validateTableQrCapabilityToken("INVALID_TOKEN_XYZ", 10L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createTableOrder(req, null, "IDEMP_KEY_002")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    // Ca 6: Quét QR hợp lệ -> mở trực tiếp menu đúng chi nhánh/bàn
    @Test
    @DisplayName("Ca 6: Quét QR từ URL root -> chuyển hướng trực tiếp tới /menu?qr=... không qua trang giới thiệu")
    void testScanningQrAtRootRedirectsDirectlyToMenu() {
        Model model = new ExtendedModelMap();
        String view = customerPageController.homePage("QR_TABLE_10", request, response, model);

        assertEquals("redirect:/menu?qr=QR_TABLE_10", view);
    }

    // Ca 7: Chỉ truyền tableId hoặc branchId -> không tạo được đơn
    @Test
    @DisplayName("Ca 7: Chỉ truyền tableId hoặc branchId không có token hợp lệ -> không tạo được đơn")
    void testPassingOnlyTableIdOrBranchIdCannotCreateOrder() {
        when(tableRepository.findByIdWithLock(10L)).thenReturn(Optional.of(table1));

        OrderDto.CreateTableOrderRequest req = OrderDto.CreateTableOrderRequest.builder()
                .tableId(10L)
                .tableToken(null)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createTableOrder(req, null, "IDEMP_KEY_003")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // Ca 8: Chọn chi nhánh trên trang giới thiệu -> xem đúng menu, không nhận quyền phiên bàn
    @Test
    @DisplayName("Ca 8: Chọn chi nhánh để xem thực đơn (/menu?branchId=1) -> xem đúng chi nhánh nhưng currentTable=null, isViewOnly=true")
    void testSelectBranchOnHomepageRendersMenuInViewOnlyMode() {
        when(settingsRepository.findById(1)).thenReturn(Optional.of(globalSettings));
        when(categoryRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());
        when(branchService.getActiveBranches()).thenReturn(List.of(BranchDto.BranchResponse.builder().id(1L).name("CN 1").build()));
        when(catalogService.getPaginatedMenu(isNull(), isNull(), isNull(), isNull(), eq("default"), eq("all"), eq(1L), eq(1), eq(12)))
                .thenReturn(CatalogDto.MenuPageResponse.builder().items(Collections.emptyList()).page(1).totalItems(0).totalPages(0).build());

        Model model = new ExtendedModelMap();
        String view = customerPageController.menuPage(null, 1L, 1, 12, "default", "all", null, null, request, response, model);

        assertEquals("customer/menu", view);
        assertEquals(1L, model.getAttribute("branchId"));
        assertNull(model.getAttribute("currentTable"));
        assertTrue((Boolean) model.getAttribute("isViewOnly"));
    }

    // Ca 9: Di chuyển Trang chủ <-> Menu trong phiên hợp lệ -> giữ đúng context
    @Test
    @DisplayName("Ca 9: Di chuyển giữa Trang chủ và Menu khi session đang OPEN -> duy trì context bàn")
    void testNavigateBetweenHomeAndMenuPreservesOpenTableSession() {
        table1.setActiveSessionId(55L);
        when(tableRepository.findByQrCode("QR_TABLE_10")).thenReturn(Optional.of(table1));
        when(request.getSession(false)).thenReturn(httpSession);
        when(httpSession.getAttribute("CURRENT_TABLE_QR")).thenReturn("QR_TABLE_10");
        when(httpSession.getAttribute("CURRENT_TABLE_SESSION_ID")).thenReturn(55L);

        TableSession activeSession = TableSession.builder().id(55L).status(TableSessionStatus.OPEN).build();
        when(sessionRepository.findById(55L)).thenReturn(Optional.of(activeSession));

        when(settingsRepository.findById(1)).thenReturn(Optional.of(globalSettings));
        when(categoryRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());
        when(branchService.getActiveBranches()).thenReturn(Collections.emptyList());
        when(catalogService.getPaginatedMenu(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(CatalogDto.MenuPageResponse.builder().items(Collections.emptyList()).page(1).totalItems(0).totalPages(0).build());

        Model model = new ExtendedModelMap();
        String view = customerPageController.menuPage(null, null, 1, 12, "default", "all", null, null, request, response, model);

        assertEquals("customer/menu", view);
        assertEquals(table1, model.getAttribute("currentTable"));
        assertFalse((Boolean) model.getAttribute("isViewOnly"));
    }

    // Ca 10: Đóng phiên rồi mở lại website trong cùng trình duyệt -> không tự gia nhập phiên mới
    @Test
    @DisplayName("Ca 10: Phiên bàn trên DB đã kết thúc (CLOSED) -> resolveTable thu hồi context, chuyển về chế độ chỉ xem")
    void testClosedSessionRevokesAccessAndDoesNotAutoJoin() {
        table1.setActiveSessionId(null); // Phiên đã đóng trên DB
        when(tableRepository.findByQrCode("QR_TABLE_10")).thenReturn(Optional.of(table1));
        when(request.getSession(false)).thenReturn(httpSession);
        when(httpSession.getAttribute("CURRENT_TABLE_QR")).thenReturn("QR_TABLE_10");
        when(httpSession.getAttribute("CURRENT_TABLE_SESSION_ID")).thenReturn(55L); // Session cũ của client

        when(settingsRepository.findById(1)).thenReturn(Optional.of(globalSettings));
        when(categoryRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());
        when(branchService.getActiveBranches()).thenReturn(Collections.emptyList());
        when(catalogService.getPaginatedMenu(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(CatalogDto.MenuPageResponse.builder().items(Collections.emptyList()).page(1).totalItems(0).totalPages(0).build());

        Model model = new ExtendedModelMap();
        String view = customerPageController.menuPage(null, null, 1, 12, "default", "all", null, null, request, response, model);

        assertEquals("customer/menu", view);
        assertNull(model.getAttribute("currentTable"));
        assertTrue((Boolean) model.getAttribute("isViewOnly"));
        verify(httpSession).removeAttribute("CURRENT_TABLE_ID");
        verify(httpSession).removeAttribute("CURRENT_TABLE_QR");
        verify(httpSession).removeAttribute("CURRENT_TABLE_SESSION_ID");
    }

    // Ca 11: Token hết hạn hoặc thu hồi -> backend từ chối
    @Test
    @DisplayName("Ca 11: Token phiên bàn đã hết hạn hoặc bị thu hồi -> backend từ chối đặt món")
    void testExpiredOrRevokedTokenRejectedByBackend() {
        table1.setActiveSessionId(99L);
        TableSession openSession = TableSession.builder().id(99L).status(TableSessionStatus.OPEN).build();
        when(tableRepository.findByIdWithLock(10L)).thenReturn(Optional.of(table1));
        when(sessionRepository.findById(99L)).thenReturn(Optional.of(openSession));

        when(guestAccessService.validateTableQrCapabilityToken("EXPIRED_TOKEN", 10L)).thenReturn(false);
        when(guestAccessService.validateTableToken("EXPIRED_TOKEN")).thenReturn(Optional.empty()); // Hết hạn / revoked

        OrderDto.CreateTableOrderRequest req = OrderDto.CreateTableOrderRequest.builder()
                .tableId(10L)
                .tableToken("EXPIRED_TOKEN")
                .items(List.of(CartDto.CartItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createTableOrder(req, null, "IDEMP_KEY_004")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    // Ca 12: Giỏ cũ hoặc idempotency key cũ -> không làm lộ đơn hoặc tạo đơn sai phiên
    @Test
    @DisplayName("Ca 12: Replay idempotency key của đơn thuộc người khác/phiên khác -> bị từ chối 403 ACCESS_DENIED")
    void testReplayIdempotencyKeyWithoutPermissionRejected() {
        when(tableRepository.findByIdWithLock(10L)).thenReturn(Optional.of(table1));
        when(guestAccessService.validateTableQrCapabilityToken("VALID_QR_TOKEN", 10L)).thenReturn(true);

        Order existingOrder = Order.builder()
                .id(999L)
                .account(Account.builder().id(888L).build()) // Đơn của tài khoản 888
                .build();

        when(idempotencyService.computeFingerprint(anyString(), anyString())).thenReturn("FP_123");
        when(idempotencyService.checkExisting(eq("CREATE_ORDER"), eq("REPLAY_KEY"), eq("FP_123"))).thenReturn(Optional.of(999L));
        when(orderRepository.findById(999L)).thenReturn(Optional.of(existingOrder));

        CurrentActor differentUser = CurrentActor.authenticated(777L, Role.CUSTOMER, null, Collections.emptyList());
        OrderDto.CreateTableOrderRequest req = OrderDto.CreateTableOrderRequest.builder()
                .tableId(10L)
                .tableToken("VALID_QR_TOKEN")
                .items(List.of(CartDto.CartItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createTableOrder(req, differentUser, "REPLAY_KEY")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không có quyền truy cập đơn hàng"));
    }

    // Ca 13: Thu ngân vẫn tạo đơn COUNTER đúng assignment; người không có quyền bị từ chối
    @Test
    @DisplayName("Ca 13: Người dùng vãng lai/customer gọi API tạo đơn COUNTER -> bị từ chối ACCESS_DENIED")
    void testCustomerCannotCreateCounterOrder() {
        CurrentActor customerActor = CurrentActor.authenticated(200L, Role.CUSTOMER, null, Collections.emptyList());
        OrderDto.CreateCounterOrderRequest req = OrderDto.CreateCounterOrderRequest.builder()
                .branchId(1L)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                orderService.createCounterOrder(req, customerActor, "COUNTER_IDEMP_01")
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chỉ nhân viên thu ngân/quản lý"));
    }

    // Ca 14: Route mapping /table/{qrCode} chuyển tiếp thẳng vào /menu?qr=...
    @Test
    @DisplayName("Ca 14: Route /table/{qrCode} chuyển hướng trực tiếp vào /menu?qr={qrCode}")
    void testTableDirectRouteRedirectsToMenu() {
        String view = customerPageController.tableQrRedirect("QR_TABLE_10");
        assertEquals("redirect:/menu?qr=QR_TABLE_10", view);
    }

    @Test
    @DisplayName("Ca 15: Khách chưa đăng nhập xem chi tiết món ở menu toàn chuỗi (không branchId) -> thành công 200")
    void testGuestViewProductDetailWholeChain() {
        CatalogDto.ProductDetailResponse mockDetail = CatalogDto.ProductDetailResponse.builder()
                .id(1L)
                .name("Trà Sữa Truyền Thống")
                .price(BigDecimal.valueOf(25000))
                .available(true)
                .build();
        when(catalogService.getProductDetail(eq(1L), isNull(), isNull())).thenReturn(mockDetail);
        when(branchService.getActiveBranches()).thenReturn(Collections.emptyList());

        Model model = new ExtendedModelMap();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        String view = customerPageController.productDetailPage("1", null, null, req, res, null, model);
        assertEquals("customer/product-detail", view);
        assertEquals(mockDetail, model.getAttribute("product"));
        assertEquals(true, model.getAttribute("isViewOnly"));
        assertNull(model.getAttribute("branchId"));
    }

    @Test
    @DisplayName("Ca 16: Khách chưa đăng nhập xem chi tiết món ở menu chi nhánh (có branchId) -> thành công 200")
    void testGuestViewProductDetailAtBranch() {
        CatalogDto.ProductDetailResponse mockDetail = CatalogDto.ProductDetailResponse.builder()
                .id(1L)
                .name("Trà Sữa Truyền Thống")
                .branchId(2L)
                .branchName("Chi nhánh Quận 2")
                .price(BigDecimal.valueOf(25000))
                .available(true)
                .build();
        when(catalogService.getProductDetail(eq(1L), eq(2L), isNull())).thenReturn(mockDetail);
        when(branchService.getActiveBranches()).thenReturn(Collections.emptyList());

        Model model = new ExtendedModelMap();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        String view = customerPageController.productDetailPage("1", 2L, null, req, res, null, model);
        assertEquals("customer/product-detail", view);
        assertEquals(mockDetail, model.getAttribute("product"));
        assertEquals(true, model.getAttribute("isViewOnly"));
        assertEquals(2L, model.getAttribute("branchId"));
    }

    @Test
    @DisplayName("Ca 17: Khách truy cập URL có query string lẫn vào id path variable (1?branchId=2) -> tự bóc tách an toàn")
    void testGuestViewProductDetailContaminatedUrl() {
        CatalogDto.ProductDetailResponse mockDetail = CatalogDto.ProductDetailResponse.builder()
                .id(1L)
                .name("Trà Sữa Truyền Thống")
                .branchId(2L)
                .branchName("Chi nhánh Quận 2")
                .price(BigDecimal.valueOf(25000))
                .available(true)
                .build();
        when(catalogService.getProductDetail(eq(1L), eq(2L), isNull())).thenReturn(mockDetail);
        when(branchService.getActiveBranches()).thenReturn(Collections.emptyList());

        Model model = new ExtendedModelMap();
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);

        String view = customerPageController.productDetailPage("1?branchId=2", null, null, req, res, null, model);
        assertEquals("customer/product-detail", view);
        assertEquals(mockDetail, model.getAttribute("product"));
        assertEquals(2L, model.getAttribute("branchId"));
    }
}
