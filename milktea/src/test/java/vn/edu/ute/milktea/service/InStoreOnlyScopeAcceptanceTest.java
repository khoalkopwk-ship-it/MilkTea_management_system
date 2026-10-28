package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.controller.OrderController;
import vn.edu.ute.milktea.controller.TableController;
import vn.edu.ute.milktea.dto.OrderDto;
import vn.edu.ute.milktea.dto.TableDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderSource;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
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
import vn.edu.ute.milktea.repository.settings.GlobalSettingsRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.table.SessionCartRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.GuestAccessService;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InStoreOnlyScopeAcceptanceTest {

    @Mock
    private DiningTableRepository tableRepository;
    @Mock
    private TableSessionRepository sessionRepository;
    @Mock
    private TableSessionService tableSessionService;
    @Mock
    private SessionCartService sessionCartService;
    @Mock
    private SessionCartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductRecipeRepository recipeRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderItemToppingRepository orderItemToppingRepository;
    @Mock
    private OrderIngredientSnapshotRepository snapshotRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentNoticeRepository paymentNoticeRepository;
    @Mock
    private RefundRequestRepository refundRepository;
    @Mock
    private CancellationRequestRepository cancellationRepository;
    @Mock
    private GlobalSettingsRepository settingsRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BusinessAuditRepository auditRepository;
    @Mock
    private GuestAccessService guestAccessService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private FixedRecipeService fixedRecipeService;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private BranchProductRepository branchProductRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private TableContextService tableContextService;

    @InjectMocks
    private OrderService orderService;

    private Branch branch1;
    private DiningTable table1;
    private vn.edu.ute.milktea.entity.catalog.Product testProduct;
    private vn.edu.ute.milktea.entity.inventory.Material testMaterial;
    private vn.edu.ute.milktea.entity.recipe.ProductRecipe testRecipe;

    @BeforeEach
    void setUp() {
        branch1 = Branch.builder().id(1L).code("CN01").name("Chi nhánh Quận 1").status("HOAT_DONG").build();
        table1 = DiningTable.builder().id(101L).name("Bàn 01").branch(branch1).qrCode("QR_CN01_B01").active(true).status(TableStatus.TRONG).build();
        testProduct = vn.edu.ute.milktea.entity.catalog.Product.builder()
                .id(10L)
                .name("Trà Sữa Oolong")
                .price(BigDecimal.valueOf(30000))
                .active(true)
                .build();
        testMaterial = vn.edu.ute.milktea.entity.inventory.Material.builder()
                .id(100L)
                .name("Cốt Trà Oolong")
                .unit("ml")
                .type(vn.edu.ute.milktea.entity.inventory.MaterialType.SOCHE)
                .build();
        testRecipe = vn.edu.ute.milktea.entity.recipe.ProductRecipe.builder()
                .id(new vn.edu.ute.milktea.entity.recipe.ProductRecipeId(10L, 100L))
                .product(testProduct)
                .material(testMaterial)
                .quantity(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    @DisplayName("Prompt 1: Endpoint online order cũ trả 410 GONE và từ chối tạo đơn")
    void testOnlineRouteRejection_Returns410Gone() {
        OrderController orderController = new OrderController(orderService);
        ResponseEntity<vn.edu.ute.milktea.common.ErrorResponse> response = orderController.rejectOnlineOrder();

        assertEquals(410, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.VALIDATION_FAILED.name(), response.getBody().getError().getCode());
        assertTrue(response.getBody().getError().getMessage().contains("ngừng hoạt động") || response.getBody().getError().getMessage().contains("TABLE/COUNTER"));
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Prompt 1: Khách hàng/Guest không được tự tạo đơn quầy COUNTER qua API")
    void testCustomerCannotCreateCounterOrder() {
        CurrentActor guest = CurrentActor.guest();
        OrderDto.CreateCounterOrderRequest req = OrderDto.CreateCounterOrderRequest.builder()
                .branchId(1L)
                .items(List.of(vn.edu.ute.milktea.dto.CartDto.CartItemRequest.builder().productId(10L).quantity(1).build()))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.createCounterOrder(req, guest, null));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());

        CurrentActor customer = CurrentActor.customer(50L, "cust@milktea.vn");
        BusinessException ex2 = assertThrows(BusinessException.class, () -> orderService.createCounterOrder(req, customer, null));
        assertEquals(ErrorCode.ACCESS_DENIED, ex2.getErrorCode());
    }

    @Test
    @DisplayName("Prompt 1: Thu ngân tạo đơn quầy COUNTER - Chi nhánh bắt buộc lấy từ phân công nhân viên")
    void testCashierCreateCounterOrder_DerivesBranchFromStaffAssignment() {
        CurrentActor cashierCn1 = CurrentActor.builder()
                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                .accountId(20L)
                .email("cashier1@milktea.vn")
                .role(Role.CASHIER)
                .branchId(1L)
                .assignedBranchIds(Set.of(1L))
                .build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(recipeRepository.findByIdProductId(10L)).thenReturn(List.of(testRecipe));
        when(branchProductRepository.findByBranchIdAndProductId(anyLong(), eq(10L))).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(999L);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(guestAccessService.issueCounterOrderToken(any(), anyInt()))
                .thenReturn(new GuestAccessService.IssuedToken("RAW_TOKEN", "hash", Instant.now().plusSeconds(86400)));

        // Request client cố tình gửi branchId=2 (sai lệch so với phân công)
        OrderDto.CreateCounterOrderRequest requestFromBrowser = OrderDto.CreateCounterOrderRequest.builder()
                .branchId(2L)
                .note("Ghi chú quầy")
                .items(List.of(vn.edu.ute.milktea.dto.CartDto.CartItemRequest.builder().productId(10L).quantity(1).build()))
                .build();

        OrderDto.OrderResponse response = orderService.createCounterOrder(requestFromBrowser, cashierCn1, null);

        // Server ghi đè lấy đúng branchId=1 của nhân viên, không tin tưởng trình duyệt
        assertEquals(1L, response.getBranchId());
        assertEquals(OrderSource.COUNTER, response.getSource());
        assertEquals(OrderStatus.CHO_THANH_TOAN, response.getStatus());
    }

    @Test
    @DisplayName("Prompt 1: Đơn TABLE gửi đơn đầu tiên mở phiên và gán đúng chi nhánh bàn")
    void testTableOrderCreatesSessionOnFirstOrder() {
        when(tableRepository.findByIdWithLock(101L)).thenReturn(Optional.of(table1));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(recipeRepository.findByIdProductId(10L)).thenReturn(List.of(testRecipe));
        when(branchProductRepository.findByBranchIdAndProductId(anyLong(), eq(10L))).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TableSession.class))).thenAnswer(inv -> {
            TableSession s = inv.getArgument(0);
            s.setId(500L);
            return s;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1001L);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(guestAccessService.issueTableSessionToken(any(), anyInt()))
                .thenReturn(new GuestAccessService.IssuedToken("RAW_TABLE_TOKEN", "hash", Instant.now().plusSeconds(86400)));

        OrderDto.CreateTableOrderRequest req = OrderDto.CreateTableOrderRequest.builder()
                .tableId(101L)
                .expectedSessionId(null) // Đơn đầu mở phiên
                .note("Ít đá, ít ngọt")
                .items(List.of(vn.edu.ute.milktea.dto.CartDto.CartItemRequest.builder().productId(10L).quantity(1).build()))
                .build();

        OrderDto.OrderResponse response = orderService.createTableOrder(req, CurrentActor.guest(), null);

        assertNotNull(response);
        assertEquals(1001L, response.getOrderId());
        assertEquals(1L, response.getBranchId());
        assertEquals(OrderSource.TABLE, response.getSource());
        assertEquals(OrderStatus.CHO_XAC_NHAN, response.getStatus());
        assertEquals(500L, response.getSessionId());
        assertEquals("RAW_TABLE_TOKEN", response.getGuestTableToken());
        verify(tableRepository).save(argThat(t -> t.getStatus() == TableStatus.CO_KHACH && Long.valueOf(500L).equals(t.getActiveSessionId())));
    }

    @Test
    @DisplayName("Prompt 1: Đọc đơn lịch sử có nguồn ONLINE tương thích hoàn toàn không gây lỗi ORM")
    void testHistoricalOnlineOrderCompatibility() {
        Account customerAcc = Account.builder().id(1L).email("admin@milktea.vn").build();
        Order legacyOrder = Order.builder()
                .id(777L)
                .account(customerAcc)
                .branch(branch1)
                .source(OrderSource.ONLINE)
                .status(OrderStatus.HOAN_THANH)
                .receiverName("Nguyễn Legacy")
                .receiverPhone("0909999999")
                .deliveryAddress("456 Võ Văn Ngân, Thủ Đức")
                .deliveryType("DELIVERY")
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(777L)).thenReturn(Optional.of(legacyOrder));

        OrderDto.OrderResponse response = orderService.getOrderForCustomer(777L, CurrentActor.customer(1L, "admin@milktea.vn"));
        assertNotNull(response);
        assertEquals(777L, response.getOrderId());
        assertEquals(OrderSource.ONLINE, response.getSource());
        assertEquals("Nguyễn Legacy", response.getReceiverName());
        assertEquals("456 Võ Văn Ngân, Thủ Đức", response.getDeliveryAddress());
        assertEquals("DELIVERY", response.getDeliveryType());
    }

    @Test
    @DisplayName("Prompt 1: getTableContext bắt buộc có mã QR, từ chối cấp ngữ cảnh không có QR")
    void testTableContext_RequiresValidQrCode() {
        TableController tableController = new TableController(tableContextService, null, tableRepository);

        BusinessException exNull = assertThrows(BusinessException.class, () -> tableController.getTableContext(null));
        assertEquals(ErrorCode.QR_INVALID, exNull.getErrorCode());

        BusinessException exBlank = assertThrows(BusinessException.class, () -> tableController.getTableContext("   "));
        assertEquals(ErrorCode.QR_INVALID, exBlank.getErrorCode());
    }
}
