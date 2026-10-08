package vn.edu.ute.milktea.sales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.config.DataInitializer;
import vn.edu.ute.milktea.dto.cancellation.CancellationDto;
import vn.edu.ute.milktea.dto.cart.CartDto;
import vn.edu.ute.milktea.dto.order.OrderDto;
import vn.edu.ute.milktea.dto.payment.PaymentDto;
import vn.edu.ute.milktea.dto.table.TableDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.Stock;
import vn.edu.ute.milktea.entity.inventory.StockId;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.repository.table.TableSessionRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.cancellation.CancellationService;
import vn.edu.ute.milktea.service.order.OrderService;
import vn.edu.ute.milktea.service.payment.PaymentService;
import vn.edu.ute.milktea.service.table.SessionCartService;
import vn.edu.ute.milktea.service.table.TableContextService;
import vn.edu.ute.milktea.service.table.TableSessionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SalesFlowIntegrationTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private TableContextService tableContextService;

    @Autowired
    private TableSessionService tableSessionService;

    @Autowired
    private SessionCartService sessionCartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CancellationService cancellationService;

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private TableSessionRepository sessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account admin;
    private Account cashier;
    private Account kitchen;
    private Account customer;
    private DiningTable testTable;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        dataInitializer.run();

        admin = accountRepository.findByEmail("admin@milktea.vn").orElseThrow();
        cashier = accountRepository.findByEmail("cashier@milktea.vn").orElseThrow();
        kitchen = accountRepository.findByEmail("kitchen@milktea.vn").orElseThrow();
        customer = accountRepository.findByEmail("customer@milktea.vn").orElseThrow();

        testTable = tableRepository.findAll().stream().filter(t -> t.getName().contains("Bàn 01")).findFirst().orElseThrow();
        sampleProduct = productRepository.findAll().stream().filter(p -> p.getName().contains("Trà Sữa")).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Luồng 1: Quét QR vào trang chủ có nhãn số bàn; xem món không đổi trạng thái bàn")
    void testFlow1_QrScanDoesNotChangeTableStatus() {
        TableDto.TableContextResponse res = tableContextService.readByQr(testTable.getQrCode());

        assertNotNull(res);
        assertEquals(testTable.getId(), res.getTableId());
        assertEquals("Bàn 01", res.getTableName());
        assertEquals(TableStatus.TRONG, res.getStatus());

        DiningTable freshTable = tableRepository.findById(testTable.getId()).orElseThrow();
        assertEquals(TableStatus.TRONG, freshTable.getStatus(), "Trạng thái bàn phải giữ nguyên TRONG khi chỉ quét QR");
        assertNull(freshTable.getActiveSessionId(), "Chưa được tạo phiên khi chỉ quét QR");
    }

    @Test
    @DisplayName("Luồng 2 & 3: Đơn đầu tiên mở phiên Có khách, giỏ server lưu trữ và kiểm soát xung đột phiên bản")
    void testFlow2And3_FirstOrderOpensSessionAndCartVersionConflict() {
        // Đặt đơn đầu tiên cho bàn
        var itemReq = CartDto.CartItemRequest.builder()
                .productId(sampleProduct.getId())
                .quantity(2)
                .note("Ít ngọt")
                .build();

        var orderReq = OrderDto.CreateTableOrderRequest.builder()
                .tableId(testTable.getId())
                .expectedSessionId(null)
                .items(List.of(itemReq))
                .build();

        String idempKey = UUID.randomUUID().toString();
        OrderDto.OrderResponse orderRes = orderService.createTableOrder(orderReq, CurrentActor.guest(), idempKey);

        assertNotNull(orderRes);
        assertNotNull(orderRes.getOrderId());
        assertNotNull(orderRes.getGuestTableToken(), "Phải cấp TableSessionToken cho khách");
        assertEquals(OrderStatus.CHO_THANH_TOAN, orderRes.getStatus());

        // Kiểm tra bàn đã chuyển Có khách và có session
        DiningTable updatedTable = tableRepository.findById(testTable.getId()).orElseThrow();
        assertEquals(TableStatus.CO_KHACH, updatedTable.getStatus());
        assertNotNull(updatedTable.getActiveSessionId());

        Long sessionId = updatedTable.getActiveSessionId();
        TableSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(TableSessionStatus.OPEN, session.getStatus());

        // Thao tác giỏ hàng phiên server
        CartDto.CartResponse currentCart = sessionCartService.getCart(sessionId);
        String currentVersion = currentCart.getVersion();

        var replaceCartReq = CartDto.ReplaceCartRequest.builder()
                .items(List.of(itemReq))
                .build();

        CartDto.CartResponse cartRes = sessionCartService.replaceCart(sessionId, replaceCartReq, currentVersion);
        assertNotNull(cartRes);

        // Thử cập nhật lại với version cũ vừa dùng (mô phỏng xung đột đa tab)
        BusinessException ex = assertThrows(BusinessException.class, () ->
                sessionCartService.replaceCart(sessionId, replaceCartReq, currentVersion));
        assertEquals(ErrorCode.CART_VERSION_CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Luồng 4: Tạo đơn tại quầy và gán vào bàn giữ nguyên mã đơn và giá trị")
    void testFlow4_CounterOrderAssignToTable() {
        var itemReq = CartDto.CartItemRequest.builder()
                .productId(sampleProduct.getId())
                .quantity(1)
                .build();

        var counterReq = OrderDto.CreateCounterOrderRequest.builder()
                .note("Đơn mang đi")
                .items(List.of(itemReq))
                .build();

        OrderDto.OrderResponse counterOrder = orderService.createCounterOrder(counterReq, CurrentActor.authenticated(cashier.getId(), Role.CASHIER), UUID.randomUUID().toString());
        assertNotNull(counterOrder.getGuestCounterToken());

        // Gán đơn quầy vào bàn Bàn 02
        DiningTable table2 = tableRepository.findAll().stream().filter(t -> t.getName().contains("Bàn 02")).findFirst().orElseThrow();
        orderService.assignCounterOrderToTable(counterOrder.getOrderId(), table2.getId(), cashier.getId());

        Order freshOrder = orderRepository.findById(counterOrder.getOrderId()).orElseThrow();
        assertNotNull(freshOrder.getSession(), "Đơn phải được liên kết với phiên của bàn 02");
        assertEquals(table2.getId(), freshOrder.getSession().getTable().getId());
        assertEquals(counterOrder.getOrderId(), freshOrder.getId(), "Mã đơn không thay đổi");
        assertEquals(OrderStatus.CHO_THANH_TOAN,freshOrder.getStatus(), "Gán bàn không làm thay đổi trạng thái thanh toán");

}

    @Test
    @DisplayName("Luồng 5: Thu ngân xác nhận -> Bếp bắt đầu -> Bếp hoàn thành toàn đơn và tiêu hao kho")
    void testFlow5_ConfirmStartCompleteFlowAndInventoryDeduction() {
        // Tạo đơn
        var itemReq = CartDto.CartItemRequest.builder()
                .productId(sampleProduct.getId())
                .quantity(2)
                .build();

        DiningTable table3 = tableRepository.findAll().stream().filter(t -> t.getName().contains("Bàn 03")).findFirst().orElseThrow();
        OrderDto.OrderResponse orderRes = orderService.createTableOrder(
                OrderDto.CreateTableOrderRequest.builder().tableId(table3.getId()).items(List.of(itemReq)).build(),
                CurrentActor.guest(),
                UUID.randomUUID().toString());

        
        Long orderId = orderRes.getOrderId();

        // Đơn mới phải chờ thanh toán
        assertEquals(
                OrderStatus.CHO_THANH_TOAN,
                orderRepository.findById(orderId).orElseThrow().getStatus());

        // Chưa thanh toán: không được xác nhận
        assertThrows(BusinessException.class,
                () -> orderService.confirm(orderId, cashier.getId()));

        // Chưa thanh toán: bếp không được bắt đầu
        assertThrows(BusinessException.class,
                () -> orderService.start(orderId, kitchen.getId()));

        // Ghi nhận chuyển khoản cho đơn tại bàn
        paymentService.recordReceipt(
                orderRes.getInvoiceId(),
                PaymentDto.RecordPaymentRequest.builder()
                        .method(PaymentMethod.BANK_TRANSFER)
                        .reference("TEST-PAID-" + orderId)
                        .build(),
                cashier.getId(),
                UUID.randomUUID().toString());

        // Đã thanh toán -> CHO_XAC_NHAN
        assertEquals(
                OrderStatus.CHO_XAC_NHAN,
                orderRepository.findById(orderId).orElseThrow().getStatus());

        // Thu ngân xác nhận
        orderService.confirm(orderId, cashier.getId());

        assertEquals(OrderStatus.CHO_CHE_BIEN, orderRepository.findById(orderId).orElseThrow().getStatus());

        // Chống lặp xác nhận (idempotent)
        assertDoesNotThrow(() -> orderService.confirm(orderId, cashier.getId()));

        // 2. Bếp bắt đầu
        orderService.start(orderId, kitchen.getId());
        assertEquals(OrderStatus.DANG_CHE_BIEN, orderRepository.findById(orderId).orElseThrow().getStatus());

        // Chống lặp bắt đầu (idempotent)
        assertDoesNotThrow(() -> orderService.start(orderId, kitchen.getId()));

        // Ghi nhận tồn kho BEP trước khi hoàn thành
        Long lyNhuaId = materialRepository.findAll().stream().filter(m -> m.getName().equals("Ly Nhựa")).findFirst().orElseThrow().getId();
        Stock lyNhuaBefore = stockRepository.findById(new StockId(lyNhuaId, StockLocation.BEP)).orElseThrow();
        BigDecimal qtyBefore = lyNhuaBefore.getQuantity();

        // 3. Bếp hoàn thành toàn đơn
        orderService.complete(orderId, kitchen.getId());
        assertEquals(OrderStatus.HOAN_THANH, orderRepository.findById(orderId).orElseThrow().getStatus());

        // Kiểm tra tồn kho BEP đã được tiêu hao chính xác (2 ly nhựa cho 2 ly trà sữa)
        Stock lyNhuaAfter = stockRepository.findById(new StockId(lyNhuaId, StockLocation.BEP)).orElseThrow();
        assertEquals(0, qtyBefore.subtract(new BigDecimal("2.000")).compareTo(lyNhuaAfter.getQuantity()),
                "Tồn kho nguyên liệu BEP phải bị trừ đúng định mức snapshot toàn đơn");
    }

    @Test
    @DisplayName("Luồng 6: Thanh toán trước/trong/sau chế biến và chống ghi thu lặp")
    void testFlow6_PaymentAndIdempotency() {
        var itemReq = CartDto.CartItemRequest.builder()
                .productId(sampleProduct.getId())
                .quantity(1)
                .build();

        DiningTable table4 = tableRepository.findAll().stream().filter(t -> t.getName().contains("Bàn 04")).findFirst().orElseThrow();
        OrderDto.OrderResponse orderRes = orderService.createTableOrder(
                OrderDto.CreateTableOrderRequest.builder().tableId(table4.getId()).items(List.of(itemReq)).build(),
                CurrentActor.guest(),
                UUID.randomUUID().toString());

        Long invoiceId = orderRes.getInvoiceId();
        String payIdempKey = "PAY_KEY_" + UUID.randomUUID();

        var payReq = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.BANK_TRANSFER)
                .reference("TIENMAT_RECEIPT_01")
                .build();

        // Thu tiền lần đầu
        PaymentDto.PaymentResponse payRes1 = paymentService.recordReceipt(invoiceId, payReq, cashier.getId(), payIdempKey);
        assertNotNull(payRes1.getPaymentId());

        // Thu tiền lần hai với cùng Idempotency-Key -> trả về kết quả cũ an toàn
        PaymentDto.PaymentResponse payRes2 = paymentService.recordReceipt(invoiceId, payReq, cashier.getId(), payIdempKey);
        assertEquals(payRes1.getPaymentId(), payRes2.getPaymentId(), "Phải trả về cùng Payment ID nếu gửi lặp khóa Idempotency-Key");

        // Gửi không có khóa hoặc khóa khác -> báo lỗi đã thanh toán
        assertThrows(BusinessException.class, () ->
                paymentService.recordReceipt(invoiceId, payReq, cashier.getId(), UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("Luồng 7: Hủy toàn đơn trước pha và tạo yêu cầu hoàn tiền khi đã thu")
    void testFlow7_CancelBeforeCookingAndRefund() {
        var itemReq = CartDto.CartItemRequest.builder()
                .productId(sampleProduct.getId())
                .quantity(1)
                .build();

        DiningTable table5 = tableRepository.findAll().stream().filter(t -> t.getName().contains("Bàn 05")).findFirst().orElseThrow();
        OrderDto.OrderResponse orderRes = orderService.createTableOrder(
                OrderDto.CreateTableOrderRequest.builder().tableId(table5.getId()).items(List.of(itemReq)).build(),
                CurrentActor.guest(),
                UUID.randomUUID().toString());

        Long orderId = orderRes.getOrderId();
        Long invoiceId = orderRes.getInvoiceId();

        // Thu tiền trước
        paymentService.recordReceipt(invoiceId,
                PaymentDto.RecordPaymentRequest.builder().method(PaymentMethod.BANK_TRANSFER).reference("MOMO123").build(),
                cashier.getId(), UUID.randomUUID().toString());

        // Khách yêu cầu hủy
        var cancelRes = cancellationService.requestCancellation(orderId,
                CancellationDto.CreateCancellationRequest.builder().reason("Khách đổi ý").build(),
                CurrentActor.guest());

        // Thu ngân duyệt hủy
        cancellationService.decideCancellation(cancelRes.getId(),
                CancellationDto.DecideCancellationRequest.builder().approved(true).reason("Đồng ý hủy").build(),
                cashier.getId());

        Order freshOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.DA_HUY, freshOrder.getStatus());
    }

    @Test
    @DisplayName("Luồng 8 & 9: Chỉ thu ngân đóng phiên khi đủ điều kiện, giải phóng bàn và chặn truy cập phiên cũ")
    void testFlow8And9_CashierCloseSessionRules() {
        // Mở bàn thủ công bởi thu ngân
        DiningTable table1 = tableRepository.findAll().stream().filter(t -> t.getName().contains("Bàn 01")).findFirst().orElseThrow();
        TableSession session = tableSessionService.openByCashier(table1.getId(), cashier.getId());

        // Tạo 1 đơn chưa thanh toán
        var itemReq = CartDto.CartItemRequest.builder()
                .productId(sampleProduct.getId())
                .quantity(1)
                .build();

        OrderDto.OrderResponse orderRes = orderService.createTableOrder(
                OrderDto.CreateTableOrderRequest.builder().tableId(table1.getId()).expectedSessionId(session.getId()).items(List.of(itemReq)).build(),
                CurrentActor.guest(),
                UUID.randomUUID().toString());

        // Cố tình đóng bàn khi đơn chưa xong -> Bị chặn
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                tableSessionService.closeByCashier(table1.getId(), cashier.getId()));
        assertEquals(ErrorCode.TABLE_CLOSE_NOT_ALLOWED, ex1.getErrorCode());

        
        // Chưa thanh toán: thu ngân không được xác nhận
        assertThrows(BusinessException.class, () ->
                orderService.confirm(orderRes.getOrderId(), cashier.getId()));

        // Chưa thanh toán: bếp không được bắt đầu
        assertThrows(BusinessException.class, () ->
                orderService.start(orderRes.getOrderId(), kitchen.getId()));

        // Đơn tại bàn chỉ được chuyển khoản
        paymentService.recordReceipt(
                orderRes.getInvoiceId(),
                PaymentDto.RecordPaymentRequest.builder()
                        .method(PaymentMethod.BANK_TRANSFER)
                        .reference("TEST-TABLE-" + orderRes.getOrderId())
                        .build(),
                cashier.getId(),
                UUID.randomUUID().toString());

        // Sau thanh toán, đơn chờ xác nhận
        assertEquals(
                OrderStatus.CHO_XAC_NHAN,
                orderRepository.findById(orderRes.getOrderId())
                        .orElseThrow().getStatus());

        // Đã thanh toán nhưng đơn chưa hoàn thành:
        // vẫn không được đóng phiên bàn
        BusinessException ex2 = assertThrows(
                BusinessException.class,
                () -> tableSessionService.closeByCashier(
                        table1.getId(), cashier.getId()));
        assertEquals(ErrorCode.TABLE_CLOSE_NOT_ALLOWED,
                ex2.getErrorCode());
        
        // Thu ngân xác nhận -> bếp bắt đầu -> hoàn thành
        orderService.confirm(orderRes.getOrderId(), cashier.getId());
        orderService.start(orderRes.getOrderId(), kitchen.getId());
        orderService.complete(orderRes.getOrderId(), kitchen.getId());

        // Đóng phiên sau khi đơn đã hoàn thành và thanh toán
        assertDoesNotThrow(() ->
                tableSessionService.closeByCashier(
                        table1.getId(), cashier.getId()));

        // Kiểm tra bàn được giải phóng
        DiningTable closedTable = tableRepository
                .findById(table1.getId())
                .orElseThrow();

        assertEquals(TableStatus.TRONG, closedTable.getStatus());
        assertNull(closedTable.getActiveSessionId());

        // Kiểm tra phiên đã đóng
        TableSession closedSession = sessionRepository
                .findById(session.getId())
                .orElseThrow();

        assertEquals(TableSessionStatus.CLOSED, closedSession.getStatus());

    }
}
