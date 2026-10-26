package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.BranchDto;
import vn.edu.ute.milktea.dto.CatalogDto;
import vn.edu.ute.milktea.dto.OrderDto;
import vn.edu.ute.milktea.dto.TableDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.branch.BranchProduct;
import vn.edu.ute.milktea.entity.catalog.Category;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.Material;
import vn.edu.ute.milktea.entity.inventory.Stock;
import vn.edu.ute.milktea.entity.inventory.StockId;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.order.*;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.StockMovementRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.order.*;
import vn.edu.ute.milktea.repository.payment.PaymentNoticeRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase3BranchOperationsTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchProductRepository branchProductRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private DiningTableRepository tableRepository;
    @Mock private TableSessionRepository sessionRepository;
    @Mock private TableSessionService tableSessionService;
    @Mock private SessionCartService sessionCartService;
    @Mock private SessionCartRepository cartRepository;
    @Mock private ProductRecipeRepository recipeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderItemToppingRepository orderItemToppingRepository;
    @Mock private OrderIngredientSnapshotRepository snapshotRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentNoticeRepository paymentNoticeRepository;
    @Mock private RefundRequestRepository refundRepository;
    @Mock private CancellationRequestRepository cancellationRepository;
    @Mock private GlobalSettingsRepository settingsRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private BusinessAuditRepository auditRepository;
    @Mock private GuestAccessService guestAccessService;
    @Mock private InventoryService inventoryService;
    @Mock private FixedRecipeService fixedRecipeService;
    @Mock private MaterialRepository materialRepository;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;
    @Mock private IdempotencyService idempotencyService;
    @Mock private StockRepository stockRepository;
    @Mock private StockMovementRepository stockMovementRepository;

    private BranchService branchService;
    private CatalogService catalogService;
    private TableContextService tableContextService;
    private OrderService orderService;

    private Branch branch1;
    private Branch branch2;

    @BeforeEach
    void setUp() {
        branchService = new BranchService(branchRepository, branchProductRepository, productRepository);
        catalogService = new CatalogService(categoryRepository, productRepository, branchProductRepository);
        tableContextService = new TableContextService(tableRepository, sessionRepository, guestAccessService);
        orderService = new OrderService(
                tableRepository, sessionRepository, tableSessionService, sessionCartService,
                cartRepository, productRepository, recipeRepository,
                orderRepository, orderItemRepository, orderItemToppingRepository, snapshotRepository,
                invoiceRepository, paymentRepository, paymentNoticeRepository, refundRepository,
                cancellationRepository, settingsRepository, accountRepository, auditRepository,
                guestAccessService, inventoryService, fixedRecipeService, materialRepository,
                realtimeEventPublisher, branchRepository, idempotencyService
        );

        branch1 = Branch.builder().id(1L).code("CN01").name("Chi nhánh Quận 1").status("HOAT_DONG").build();
        branch2 = Branch.builder().id(2L).code("CN02").name("Chi nhánh Thủ Đức").status("HOAT_DONG").build();
    }

    @Test
    @DisplayName("BranchService: Manager chỉ có thể bật/tắt món tại chi nhánh mình quản lý")
    void testManagerBranchProductAvailabilityAccess() {
        CurrentActor managerBranch1 = CurrentActor.authenticated(10L, Role.MANAGER, 1L, List.of(1L));
        Product product = Product.builder().id(5L).name("Trà Sữa Trân Châu").active(true).build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(branchProductRepository.findByBranchIdAndProductId(1L, 5L)).thenReturn(Optional.empty());

        // Cho phép tại CN01
        assertDoesNotThrow(() -> branchService.updateProductAvailability(1L, 5L, false, managerBranch1));
        verify(branchProductRepository, times(1)).save(any(BranchProduct.class));

        // Từ chối tại CN02
        BusinessException ex = assertThrows(BusinessException.class,
                () -> branchService.updateProductAvailability(2L, 5L, false, managerBranch1));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("TableContextService: Quét QR trả về thông tin chi nhánh của bàn")
    void testTableContextReturnsBranchInfo() {
        DiningTable table = DiningTable.builder()
                .id(101L)
                .name("Bàn 01")
                .qrCode("QR_TABLE_101")
                .active(true)
                .branch(branch1)
                .build();

        when(tableRepository.findByQrCode("QR_TABLE_101")).thenReturn(Optional.of(table));

        TableDto.TableContextResponse response = tableContextService.readByQr("QR_TABLE_101");
        assertNotNull(response);
        assertEquals(101L, response.getTableId());
        assertEquals("Bàn 01", response.getTableName());
        assertEquals(1L, response.getBranchId());
        assertEquals("Chi nhánh Quận 1", response.getBranchName());
    }

    @Test
    @DisplayName("CatalogService: Menu lọc theo chi nhánh hiển thị đúng trạng thái món")
    void testCatalogMenuReflectsBranchProductAvailability() {
        Category cat = Category.builder().id(1L).name("Trà sữa").active(true).allowTopping(true).build();
        Product prod1 = Product.builder().id(10L).name("Trà Sữa Oolong").price(BigDecimal.valueOf(35000)).category(cat).active(true).build();
        Product prod2 = Product.builder().id(11L).name("Trà Sữa Lài").price(BigDecimal.valueOf(32000)).category(cat).active(true).build();

        when(categoryRepository.findAllByActiveTrue()).thenReturn(List.of(cat));
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(prod1, prod2));

        // Chi nhánh 1: prod1 bị tắt (available = false)
        BranchProduct bpDisabled = BranchProduct.builder()
                .branch(branch1)
                .product(prod1)
                .available(false)
                .build();
        when(branchProductRepository.findByBranchId(1L)).thenReturn(List.of(bpDisabled));

        CatalogDto.MenuResponse menu = catalogService.getMenu(1L);
        assertNotNull(menu);
        assertEquals(1, menu.getCategories().size());
        var products = menu.getCategories().get(0).getProducts();
        assertEquals(2, products.size());

        // Kiểm tra prod1 đã bị đánh dấu available = false tại CN01
        var p1Item = products.stream().filter(p -> p.getId().equals(10L)).findFirst().orElseThrow();
        assertFalse(p1Item.isAvailable());

        // prod2 không có cấu hình tắt -> mặc định available = true
        var p2Item = products.stream().filter(p -> p.getId().equals(11L)).findFirst().orElseThrow();
        assertTrue(p2Item.isAvailable());
    }

    @Test
    @DisplayName("OrderService: Tạo đơn trực tuyến gán đúng chi nhánh và sinh guest token")
    void testCreateOnlineOrderSuccess() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));

        Product product = Product.builder().id(10L).name("Trà Sữa Oolong").size("M").price(BigDecimal.valueOf(35000)).active(true).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1001L);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(2001L);
            return i;
        });
        when(guestAccessService.issueCounterOrderToken(any(Order.class), anyInt()))
                .thenReturn(new GuestAccessService.IssuedToken("RAW_ONLINE_TOKEN", "hash", Instant.now().plusSeconds(86400)));

        OrderDto.CreateOnlineOrderRequest req = OrderDto.CreateOnlineOrderRequest.builder()
                .branchId(1L)
                .customerName("Nguyễn Văn A")
                .customerPhone("0901234567")
                .deliveryAddress("123 Lê Duẩn, Q1")
                .items(List.of(vn.edu.ute.milktea.dto.CartDto.CartItemRequest.builder().productId(10L).quantity(2).build()))
                .build();

        CurrentActor customer = CurrentActor.customer(50L, "customer@milktea.vn");
        OrderDto.OrderResponse response = orderService.createOnlineOrder(req, customer, null);

        assertNotNull(response);
        assertEquals(1001L, response.getOrderId());
        assertEquals(1L, response.getBranchId());
        assertEquals("Chi nhánh Quận 1", response.getBranchName());
        assertEquals(OrderSource.ONLINE, response.getSource());
        assertEquals(OrderStatus.CHO_XAC_NHAN, response.getStatus());
        assertEquals("RAW_ONLINE_TOKEN", response.getGuestCounterToken());

        // Kiểm tra sự kiện realtime được gửi đến kênh chi nhánh
        verify(realtimeEventPublisher, atLeastOnce()).publishAfterCommit(
                eq("/topic/branches/1/cashier"), eq("ORDER_CREATED"), eq("1001"), isNull(), anyString(), anyMap());
    }

    @Test
    @DisplayName("OrderService: Gắn đơn quầy vào bàn từ chối nếu khác chi nhánh")
    void testAssignCounterOrderToTable_RejectsCrossBranch() {
        Order counterOrderBranch1 = Order.builder()
                .id(501L)
                .source(OrderSource.COUNTER)
                .branch(branch1)
                .build();

        DiningTable tableBranch2 = DiningTable.builder()
                .id(202L)
                .name("Bàn CN02")
                .branch(branch2)
                .build();

        when(orderRepository.findByIdWithLock(501L)).thenReturn(Optional.of(counterOrderBranch1));
        when(tableRepository.findById(202L)).thenReturn(Optional.of(tableBranch2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.assignCounterOrderToTable(501L, 202L, 99L));
        assertEquals(ErrorCode.ORDER_STATE_CONFLICT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("không cùng một chi nhánh"));
    }

    @Test
    @DisplayName("InventoryService: Trừ kho BEP độc lập theo từng chi nhánh khi Bếp hoàn thành đơn")
    void testInventoryServiceCompleteOrderDeductionIsBranchScoped() {
        InventoryService invService = new InventoryService(
                stockRepository, stockMovementRepository, materialRepository, accountRepository,
                null, null, null, null, snapshotRepository, orderItemRepository,
                recipeRepository, null, null, null, null, auditRepository, realtimeEventPublisher);

        Material mat = Material.builder().id(100L).name("Cốt Trà Oolong").type(vn.edu.ute.milktea.entity.inventory.MaterialType.SOCHE).unit("ml").build();
        Order orderBranch2 = Order.builder().id(999L).branch(branch2).build();

        OrderIngredientSnapshot snap = OrderIngredientSnapshot.builder()
                .id(new OrderIngredientSnapshotId(999L, 100L))
                .order(orderBranch2)
                .material(mat)
                .snapshotType(vn.edu.ute.milktea.entity.inventory.MaterialType.SOCHE)
                .totalQuantity(BigDecimal.valueOf(200))
                .build();

        when(snapshotRepository.findByIdOrderId(999L)).thenReturn(List.of(snap));

        // Kho CN02 có tồn 500
        Stock stockBranch2 = Stock.builder()
                .id(StockId.builder().branchId(2L).materialId(100L).location(StockLocation.BEP).build())
                .branch(branch2)
                .material(mat)
                .quantity(BigDecimal.valueOf(500))
                .build();

        when(stockRepository.findByIdWithLock(2L, 100L, StockLocation.BEP))
                .thenReturn(Optional.of(stockBranch2));

        Account kitchenUser = Account.builder().id(20L).fullName("Bếp CN02").build();
        invService.completeOrderConsumption(orderBranch2, kitchenUser);

        // Kho của CN02 bị trừ 200, còn lại 300
        assertEquals(BigDecimal.valueOf(300), stockBranch2.getQuantity());
        verify(stockRepository, times(1)).save(stockBranch2);

        // Kiểm tra không bao giờ đụng đến kho của CN01
        verify(stockRepository, never()).findByIdWithLock(eq(1L), anyLong(), any(StockLocation.class));
    }
}
