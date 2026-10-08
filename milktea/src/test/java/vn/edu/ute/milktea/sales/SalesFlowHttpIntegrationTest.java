package vn.edu.ute.milktea.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import vn.edu.ute.milktea.config.DataInitializer;
import vn.edu.ute.milktea.dto.cancellation.CancellationDto;
import vn.edu.ute.milktea.dto.cart.CartDto;
import vn.edu.ute.milktea.dto.order.OrderDto;
import vn.edu.ute.milktea.dto.payment.PaymentDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.Stock;
import vn.edu.ute.milktea.entity.inventory.StockId;
import vn.edu.ute.milktea.entity.inventory.StockLocation;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.entity.table.DiningTable;
import vn.edu.ute.milktea.entity.table.TableStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.JwtService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SalesFlowHttpIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private DiningTableRepository tableRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtService jwtService;

    private String cashierToken;
    private String kitchenToken;
    private String adminToken;
    private DiningTable tableB01;
    private Product traSuaTruyenThong;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        dataInitializer.run();

        tableB01 = tableRepository.findAll().stream()
                .filter(t -> t.getName().contains("Bàn 01"))
                .findFirst()
                .orElseThrow();
        traSuaTruyenThong = productRepository.findAll().stream()
                .filter(p -> p.getName().contains("Truyền Thống"))
                .findFirst()
                .orElseThrow();

        // Nạp đủ tồn kho bếp cho bài test
        materialRepository.findAll().forEach(mat -> {
            Stock stock = stockRepository.findById(new StockId(mat.getId(), StockLocation.BEP))
                    .orElse(Stock.builder()
                            .id(new StockId(mat.getId(), StockLocation.BEP))
                            .material(mat)
                            .quantity(BigDecimal.valueOf(50000))
                            .threshold(BigDecimal.TEN)
                            .build());
            stock.setQuantity(BigDecimal.valueOf(50000));
            stockRepository.save(stock);
        });

        Account cashier = accountRepository.findByEmail("cashier@milktea.vn").orElseThrow();
        Account kitchen = accountRepository.findByEmail("kitchen@milktea.vn").orElseThrow();
        Account admin = accountRepository.findByEmail("admin@milktea.vn").orElseThrow();

        cashierToken = "Bearer " + jwtService.generateToken(cashier);
        kitchenToken = "Bearer " + jwtService.generateToken(kitchen);
        adminToken = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    @DisplayName("Luồng 1: Quét QR -> Xem Menu; bàn giữ nguyên trạng thái TRONG, không mở phiên trước")
    void test1_qrScan_viewCatalog_tableRemainsTrong() throws Exception {
        mockMvc.perform(get("/api/v1/public/table-context")
                        .param("qrCode", tableB01.getQrCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tableName").value("Bàn 01"))
                .andExpect(jsonPath("$.data.status").value("TRONG"))
                .andExpect(jsonPath("$.data.sessionId").doesNotExist());

        mockMvc.perform(get("/api/v1/public/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));

        DiningTable currentTable = tableRepository.findById(tableB01.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(TableStatus.TRONG, currentTable.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(currentTable.getActiveSessionId());
    }

    @Test
    @DisplayName("Luồng 2 & 3: Thu ngân mở phiên -> Khách thêm giỏ có ETag -> Xung đột phiên bản giỏ (If-Match 409)")
    void test2_openSession_and_cartEtagConcurrency() throws Exception {
        // Thu ngân mở phiên
        String sessionRes = mockMvc.perform(post("/api/v1/cashier/tables/{id}/open-session", tableB01.getId())
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CO_KHACH"))
                .andReturn().getResponse().getContentAsString();

        Long sessionId = objectMapper.readTree(sessionRes).path("data").path("activeSessionId").asLong();

        // Lấy giỏ hàng qua GET /api/v1/table-sessions/{sessionId}/cart
        String cartRes = mockMvc.perform(get("/api/v1/table-sessions/{sessionId}/cart", sessionId))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn().getResponse().getContentAsString();

        // Cập nhật giỏ hợp lệ với If-Match: "1"
        CartDto.ReplaceCartRequest replaceReq = CartDto.ReplaceCartRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(traSuaTruyenThong.getId())
                        .quantity(2)
                        .note("Ít đường")
                        .build()))
                .build();

        mockMvc.perform(put("/api/v1/table-sessions/{sessionId}/cart", sessionId)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replaceReq)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // Cập nhật giỏ với version cũ "1" -> 409 Conflict
        mockMvc.perform(put("/api/v1/table-sessions/{sessionId}/cart", sessionId)
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replaceReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CART_VERSION_CONFLICT"));
    }

    @Test
    @DisplayName("Luồng 4: Đặt đơn bàn -> Chống gửi đơn lặp qua Idempotency-Key")
    void test3_createTableOrder_and_idempotency() throws Exception {
        OrderDto.CreateTableOrderRequest orderReq = OrderDto.CreateTableOrderRequest.builder()
                .tableId(tableB01.getId())
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(traSuaTruyenThong.getId())
                        .quantity(2)
                        .note("50% đá")
                        .build()))
                .build();

        String idempotencyKey = "IDEM-TEST-ORD-" + UUID.randomUUID();

        // Gửi lần 1 -> 201 Created
        String res1 = mockMvc.perform(post("/api/v1/orders/table")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.guestTableToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Long orderId1 = objectMapper.readTree(res1).path("data").path("orderId").asLong();

        // Gửi lần 2 với cùng key -> Trả về đúng đơn đã tạo, không báo lỗi và không sinh đơn mới
        String res2 = mockMvc.perform(post("/api/v1/orders/table")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        Long orderId2 = objectMapper.readTree(res2).path("data").path("orderId").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(orderId1, orderId2);
    }

    @Test
    @DisplayName("Luồng 5: Đơn quầy -> Thu ngân gán bàn -> Giữ nguyên mã đơn và tiền")
    void test4_createCounterOrder_and_assignTable() throws Exception {
        OrderDto.CreateCounterOrderRequest counterReq = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(traSuaTruyenThong.getId())
                        .quantity(1)
                        .build()))
                .build();

        String res = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(counterReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.source").value("COUNTER"))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).path("data").path("orderId").asLong();

        // Thu ngân gán bàn
        OrderDto.AssignTableRequest assignReq = OrderDto.AssignTableRequest.builder()
                .tableId(tableB01.getId())
                .build();

        mockMvc.perform(post("/api/v1/cashier/orders/{id}/assign-table", orderId)
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra đơn sau khi gán bàn
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.tableId").value(tableB01.getId()))
                .andExpect(jsonPath("$.data.status").value("CHO_XAC_NHAN"));
    }

    @Test
    @DisplayName("Luồng 6: Thu ngân xác nhận -> Bếp bắt đầu -> Bếp hoàn thành toàn đơn (tiêu hao nguyên liệu)")
    void test5_kitchenFlow_confirm_start_complete() throws Exception {
        OrderDto.CreateTableOrderRequest orderReq = OrderDto.CreateTableOrderRequest.builder()
                .tableId(tableB01.getId())
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(traSuaTruyenThong.getId())
                        .quantity(1)
                        .build()))
                .build();

        String res = mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).path("data").path("orderId").asLong();

        // Thu ngân xác nhận
        mockMvc.perform(post("/api/v1/cashier/orders/{id}/confirm", orderId)
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Bếp kiểm tra danh sách đơn
        mockMvc.perform(get("/api/v1/kitchen/orders")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));

        // Bếp bắt đầu làm
        mockMvc.perform(post("/api/v1/kitchen/orders/{id}/start", orderId)
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Bếp hoàn thành toàn đơn
        mockMvc.perform(post("/api/v1/kitchen/orders/{id}/complete", orderId)
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra trạng thái đơn là HOAN_THANH
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HOAN_THANH"));
    }

    @Test
    @DisplayName("Luồng 7: Ghi thu thanh toán linh hoạt -> Chống ghi thu lặp qua Idempotency-Key")
    void test6_payment_and_idempotency() throws Exception {
        OrderDto.CreateTableOrderRequest orderReq = OrderDto.CreateTableOrderRequest.builder()
                .tableId(tableB01.getId())
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(traSuaTruyenThong.getId())
                        .quantity(2)
                        .build()))
                .build();

        String res = mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(res).path("data").path("orderId").asLong();
        Long invoiceId = objectMapper.readTree(res).path("data").path("invoiceId").asLong();

        PaymentDto.RecordPaymentRequest payReq = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.CASH)
                .reference("TIEN-MAT-QUAY")
                .build();

        String idempotencyKey = "IDEM-PAY-" + UUID.randomUUID();

        // Ghi thu lần 1 -> 201 Created
        mockMvc.perform(post("/api/v1/cashier/orders/{id}/payments", orderId)
                        .header("Authorization", cashierToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").exists());

        // Ghi thu lại cùng key -> 201 Created (idempotent, không ném lỗi)
        mockMvc.perform(post("/api/v1/cashier/orders/{id}/payments", orderId)
                        .header("Authorization", cashierToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Ghi thu lần nữa không dùng key cũ -> 409 Conflict (đã thanh toán)
        mockMvc.perform(post("/api/v1/cashier/orders/{id}/payments", orderId)
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_ALREADY_RECORDED"));
    }

    @Test
    @DisplayName("Luồng 8 & 9: Hủy đơn trước pha -> Hoàn tiền đúng quy tắc -> Thu ngân đóng phiên bàn thành công")
    void test7_cancellation_refund_and_closeSession() throws Exception {
        // 1. Tạo đơn bàn
        OrderDto.CreateTableOrderRequest orderReq = OrderDto.CreateTableOrderRequest.builder()
                .tableId(tableB01.getId())
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(traSuaTruyenThong.getId())
                        .quantity(1)
                        .build()))
                .build();

        String orderRes = mockMvc.perform(post("/api/v1/orders/table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderRes).path("data").path("orderId").asLong();
        Long sessionId = objectMapper.readTree(orderRes).path("data").path("sessionId").asLong();

        // 2. Thu ngân ghi thu tiền mặt trước khi pha chế
        PaymentDto.RecordPaymentRequest payReq = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.CASH)
                .reference("TIEN-MAT-TRUOC-PHA")
                .build();

        mockMvc.perform(post("/api/v1/cashier/orders/{id}/payments", orderId)
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        // 3. Khách yêu cầu hủy trước pha chế
        CancellationDto.CreateCancellationRequest cancelReq = CancellationDto.CreateCancellationRequest.builder()
                .reason("Khách đổi ý muốn hủy")
                .build();

        String cancelRes = mockMvc.perform(post("/api/v1/orders/{id}/cancellation-requests", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long requestId = objectMapper.readTree(cancelRes).path("data").path("id").asLong();

        // 4. Thu ngân duyệt hủy đơn
        CancellationDto.DecideCancellationRequest decideReq = CancellationDto.DecideCancellationRequest.builder()
                .approved(true)
                .reason("Đồng ý hủy đơn vì bếp chưa pha")
                .build();

        mockMvc.perform(post("/api/v1/cashier/cancellation-requests/{id}/decision", requestId)
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decideReq)))
                .andExpect(status().isOk());

        // 5. Admin xem danh sách hoàn tiền và duyệt hoàn tiền
        String refundListRes = mockMvc.perform(get("/api/v1/admin/refunds")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andReturn().getResponse().getContentAsString();

        Long refundId = objectMapper.readTree(refundListRes).path("data").get(0).path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/refunds/{id}/decision", refundId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", true, "reason", "Duyệt hoàn tiền cho khách"))))
                .andExpect(status().isOk());

        // 6. Thu ngân thực hiện chi hoàn tiền
        mockMvc.perform(post("/api/v1/cashier/refunds/{id}/record-refund", refundId)
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reference", "CHI-HOAN-TM-01"))))
                .andExpect(status().isOk());

        // 7. Thu ngân đóng phiên bàn thủ công
        mockMvc.perform(post("/api/v1/cashier/table-sessions/{sessionId}/close", sessionId)
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // Kiểm tra bàn trở về TRONG
        DiningTable closedTable = tableRepository.findById(tableB01.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(TableStatus.TRONG, closedTable.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(closedTable.getActiveSessionId());

        // Kiểm tra gọi lại giỏ hàng của phiên đã đóng -> Bị từ chối
        mockMvc.perform(get("/api/v1/table-sessions/{sessionId}/cart", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TABLE_SESSION_CLOSED"));
    }
}
