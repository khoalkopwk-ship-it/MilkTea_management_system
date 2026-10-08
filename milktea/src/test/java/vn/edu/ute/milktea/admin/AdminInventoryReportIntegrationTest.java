package vn.edu.ute.milktea.admin;

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
import vn.edu.ute.milktea.dto.admin.AdminDto;
import vn.edu.ute.milktea.dto.cancellation.CancellationDto;
import vn.edu.ute.milktea.dto.cart.CartDto;
import vn.edu.ute.milktea.dto.inventory.InventoryDto;
import vn.edu.ute.milktea.dto.order.OrderDto;
import vn.edu.ute.milktea.dto.payment.PaymentDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.ProductRecipe;
import vn.edu.ute.milktea.entity.recipe.ProductRecipeId;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.MaterialRepository;
import vn.edu.ute.milktea.repository.inventory.StockMovementRepository;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.JwtService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminInventoryReportIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private vn.edu.ute.milktea.repository.account.AccountRepository accountRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.catalog.CategoryRepository categoryRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.catalog.ProductRepository productRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.inventory.MaterialRepository materialRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.inventory.StockRepository stockRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.inventory.StockMovementRepository movementRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository productRecipeRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository preparationRecipeRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository preparationRecipeItemRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.table.DiningTableRepository tableRepository;

    @Autowired
    private vn.edu.ute.milktea.repository.order.OrderRepository orderRepository;

    @Autowired
    private vn.edu.ute.milktea.security.JwtService jwtService;

    private String adminToken;
    private String cashierToken;
    private String kitchenToken;
    private String customerToken;

    private Account adminAccount;
    private Account cashierAccount;
    private Account kitchenAccount;
    private Account customerAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        dataInitializer.run();

        adminAccount = accountRepository.findByEmail("admin@milktea.vn").orElseThrow();
        cashierAccount = accountRepository.findByEmail("cashier@milktea.vn").orElseThrow();
        kitchenAccount = accountRepository.findByEmail("kitchen@milktea.vn").orElseThrow();

        customerAccount = accountRepository.findByEmail("customer_test@milktea.vn")
                .orElseGet(() -> accountRepository.save(Account.builder()
                        .email("customer_test@milktea.vn")
                        .passwordHash("$2a$10$hashedPasswordForTestCustomer1234567890")
                        .fullName("Khách Test")
                        .role(Role.CUSTOMER)
                        .active(true)
                        .tokenVersion(1L)
                        .build()));

        adminToken = "Bearer " + jwtService.generateToken(adminAccount);
        cashierToken = "Bearer " + jwtService.generateToken(cashierAccount);
        kitchenToken = "Bearer " + jwtService.generateToken(kitchenAccount);
        customerToken = "Bearer " + jwtService.generateToken(customerAccount);
    }

    @Test
    @DisplayName("1. Quản trị & Phân quyền: Admin quản lý nhân viên, danh mục, sản phẩm, công thức, bàn; chặn CUSTOMER")
    void testAdminSecurityAndManagement() throws Exception {
        // 1. Chặn CUSTOMER truy cập Admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/accounts")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // 2. Admin tạo tài khoản nhân viên mới (CASHIER)
        AdminDto.CreateStaffRequest createStaffReq = AdminDto.CreateStaffRequest.builder()
                .email("newcashier_" + UUID.randomUUID() + "@milktea.vn")
                .password("Password123@")
                .fullName("Thu Ngân Mới")
                .phone("0912345678")
                .role(Role.CASHIER)
                .build();

        String staffRespStr = mockMvc.perform(post("/api/v1/admin/accounts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createStaffReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(createStaffReq.getEmail()))
                .andExpect(jsonPath("$.data.role").value("CASHIER"))
                .andReturn().getResponse().getContentAsString();

        Long newStaffId = objectMapper.readTree(staffRespStr).path("data").path("id").asLong();

        // 3. Admin cập nhật vai trò nhân viên -> tokenVersion được tăng để thu hồi token cũ
        AdminDto.UpdateStaffRequest updateStaffReq = AdminDto.UpdateStaffRequest.builder()
                .role(Role.KITCHEN)
                .build();

        mockMvc.perform(put("/api/v1/admin/accounts/" + newStaffId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStaffReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("KITCHEN"));

        // 4. Admin tạo danh mục mới
        AdminDto.CategoryRequest catReq = AdminDto.CategoryRequest.builder()
                .name("Trà Trái Cây Mới")
                .active(true)
                .build();

        String catRespStr = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Trà Trái Cây Mới"))
                .andReturn().getResponse().getContentAsString();

        Long newCatId = objectMapper.readTree(catRespStr).path("data").path("id").asLong();

        // 5. Admin tạo sản phẩm mới và công thức tiêu hao
        AdminDto.ProductRequest prodReq = AdminDto.ProductRequest.builder()
                .categoryId(newCatId)
                .name("Trà Đào Cam Sả Đặc Biệt")
                .size("M")
                .price(BigDecimal.valueOf(45000))
                .description("Trà đào đậm vị")
                .active(true)
                .build();

        String prodRespStr = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long newProdId = objectMapper.readTree(prodRespStr).path("data").path("id").asLong();

        Material traDen = materialRepository.findAll().stream().findFirst().orElseThrow();
        AdminDto.UpdateProductRecipeRequest recipeReq = AdminDto.UpdateProductRecipeRequest.builder()
                .items(List.of(AdminDto.RecipeItemDto.builder()
                        .materialId(traDen.getId())
                        .quantity(BigDecimal.valueOf(25.0))
                        .build()))
                .build();

        mockMvc.perform(put("/api/v1/admin/products/" + newProdId + "/recipe")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recipeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ingredients[0].materialId").value(traDen.getId()))
                .andExpect(jsonPath("$.data.ingredients[0].quantity").value(25.0));

        // 6. Admin tạo bàn mới và lấy mã QR
        AdminDto.TableRequest tableReq = AdminDto.TableRequest.builder()
                .name("Bàn VIP 99")
                .active(true)
                .build();

        String tableRespStr = mockMvc.perform(post("/api/v1/admin/tables")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Bàn VIP 99"))
                .andReturn().getResponse().getContentAsString();

        Long newTableId = objectMapper.readTree(tableRespStr).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/tables/" + newTableId + "/qr")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qrCode").isNotEmpty());
    }

    @Test
    @DisplayName("2. Vận hành Kho: Nhập KHO -> Xuất KHO sang BEP (không cần duyệt) -> Mẻ sơ chế -> Hao hụt -> Lịch sử biến động")
    void testInventoryLifecycle_Import_Transfer_Batch_Waste() throws Exception {
        Material traDen = materialRepository.findAll().stream()
                .filter(m -> m.getType() == MaterialType.THO)
                .findFirst().orElseThrow();

        // 1. Nhập kho vào KHO qua /api/v1/inventory/imports
        InventoryDto.CreateImportReceiptRequest importReq = InventoryDto.CreateImportReceiptRequest.builder()
                .supplier("Nhà Cung Cấp Nông Sản")
                .reason("Nhập nguyên liệu đầu tuần")
                .items(List.of(
                        InventoryDto.ImportItemRequest.builder()
                                .materialId(traDen.getId())
                                .quantity(BigDecimal.valueOf(100.0))
                                .unitPrice(BigDecimal.valueOf(50000))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/inventory/imports")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isCreated());

        // Kiểm tra tồn KHO >= 100
        Stock khoStock = stockRepository.findById(new StockId(traDen.getId(), StockLocation.KHO)).orElseThrow();
        assertThat(khoStock.getQuantity()).isGreaterThanOrEqualTo(BigDecimal.valueOf(100.0));

        // 2. KITCHEN xuất chuyển từ KHO sang BEP không cần admin duyệt qua /api/v1/inventory/issues
        BigDecimal bepInitial = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP))
                .map(Stock::getQuantity).orElse(BigDecimal.ZERO);

        InventoryDto.CreateStockIssueRequest issueReq = InventoryDto.CreateStockIssueRequest.builder()
                .reason("Bếp yêu cầu xuất pha chế")
                .items(List.of(
                        InventoryDto.StockIssueItemRequest.builder()
                                .materialId(traDen.getId())
                                .quantity(BigDecimal.valueOf(30.0))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/inventory/issues")
                        .header("Authorization", kitchenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueReq)))
                .andExpect(status().isCreated());

        Stock bepAfterIssue = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP)).orElseThrow();
        assertThat(bepAfterIssue.getQuantity()).isEqualByComparingTo(bepInitial.add(BigDecimal.valueOf(30.0)));

        // 3. Mẻ sơ chế (Preparation Batch) tại BEP
        var prepRecipe = preparationRecipeRepository.findAll().stream().findFirst();
        if (prepRecipe.isPresent()) {
            PreparationRecipe pr = prepRecipe.get();
            var items = preparationRecipeItemRepository.findByIdRecipeId(pr.getId());
            // Đảm bảo đủ nguyên liệu đầu vào tại BEP
            items.forEach(it -> {
                Stock st = stockRepository.findById(new StockId(it.getInputMaterial().getId(), StockLocation.BEP))
                        .orElseGet(() -> stockRepository.save(Stock.builder()
                                .id(new StockId(it.getInputMaterial().getId(), StockLocation.BEP))
                                .material(it.getInputMaterial())
                                .quantity(BigDecimal.ZERO)
                                .threshold(BigDecimal.ZERO)
                                .build()));
                st.setQuantity(st.getQuantity().add(BigDecimal.valueOf(500.0)));
                stockRepository.save(st);
            });

            InventoryDto.CreatePreparationBatchRequest batchReq = InventoryDto.CreatePreparationBatchRequest.builder()
                    .recipeId(pr.getId())
                    .actualQuantity(BigDecimal.valueOf(10.0))
                    .build();

            mockMvc.perform(post("/api/v1/inventory/batches")
                            .header("Authorization", kitchenToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(batchReq)))
                    .andExpect(status().isCreated());

            Stock outputStock = stockRepository.findById(new StockId(pr.getOutputMaterial().getId(), StockLocation.BEP)).orElseThrow();
            assertThat(outputStock.getQuantity()).isGreaterThanOrEqualTo(BigDecimal.valueOf(10.0));
        }

        // 4. Ghi nhận hao hụt (Waste)
        InventoryDto.CreateWasteRequest wasteReq = InventoryDto.CreateWasteRequest.builder()
                .location(StockLocation.BEP)
                .reason("Làm rơi vãi nguyên liệu trong ca")
                .items(List.of(
                        InventoryDto.WasteItemRequest.builder()
                                .materialId(traDen.getId())
                                .quantity(BigDecimal.valueOf(2.0))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/inventory/waste")
                        .header("Authorization", kitchenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wasteReq)))
                .andExpect(status().isCreated());

        // 5. Kiểm tra lịch sử biến động kho qua /api/v1/inventory/movements
        mockMvc.perform(get("/api/v1/inventory/movements")
                        .header("Authorization", kitchenToken)
                        .param("materialId", traDen.getId().toString())
                        .param("location", "BEP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("3. Thiếu tồn kho BEP khi bếp hoàn thành đơn -> 409 Conflict, rollback trạng thái và kho")
    void testInsufficientInventoryOnKitchenComplete_RollsBack() throws Exception {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        Material traDen = materialRepository.findAll().stream().findFirst().orElseThrow();

        // Cấu hình công thức sản phẩm cần 100 đơn vị nguyên liệu
        productRecipeRepository.deleteAll();
        productRecipeRepository.flush();
        productRecipeRepository.saveAndFlush(ProductRecipe.builder()
                .id(new ProductRecipeId(product.getId(), traDen.getId()))
                .product(product)
                .material(traDen)
                .quantity(BigDecimal.valueOf(100.0))
                .build());

        // Đặt tồn kho BEP = 5 (thiếu so với 100 cần tiêu hao)
        Stock bepStock = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP))
                .orElseGet(() -> Stock.builder()
                        .id(new StockId(traDen.getId(), StockLocation.BEP))
                        .material(traDen)
                        .threshold(BigDecimal.ZERO)
                        .build());
        bepStock.setQuantity(BigDecimal.valueOf(5.0));
        stockRepository.saveAndFlush(bepStock);

        // Tạo đơn tại quầy
        OrderDto.CreateCounterOrderRequest orderReq = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        String orderRespStr = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderRespStr).path("data").path("orderId").asLong();

        // Cashier xác nhận -> Kitchen bắt đầu nấu
        mockMvc.perform(post("/api/v1/cashier/orders/" + orderId + "/confirm")
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/start")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        // Bếp gọi complete khi thiếu tồn kho BEP -> 409 Conflict với INVENTORY_INSUFFICIENT
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/complete")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVENTORY_INSUFFICIENT"));

        // Kiểm tra sau rollback: Đơn hàng vẫn ở trạng thái DANG_CHE_BIEN, kho BEP vẫn giữ nguyên 5.0
        Order orderAfter = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.DANG_CHE_BIEN);

        Stock bepAfter = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP)).orElseThrow();
        assertThat(bepAfter.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
    }

    @Test
    @DisplayName("4. Bếp hoàn thành đơn tiêu hao kho cùng transaction và idempotent (chỉ trừ 1 lần)")
    void testSingleInventoryConsumptionAndCompleteIdempotency() throws Exception {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        Material traDen = materialRepository.findAll().stream().findFirst().orElseThrow();

        productRecipeRepository.deleteAll();
        productRecipeRepository.flush();
        productRecipeRepository.saveAndFlush(ProductRecipe.builder()
                .id(new ProductRecipeId(product.getId(), traDen.getId()))
                .product(product)
                .material(traDen)
                .quantity(BigDecimal.valueOf(20.0))
                .build());

        // Đặt tồn kho BEP = 100.0
        Stock bepStock = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP))
                .orElseGet(() -> Stock.builder()
                        .id(new StockId(traDen.getId(), StockLocation.BEP))
                        .material(traDen)
                        .threshold(BigDecimal.ZERO)
                        .build());
        bepStock.setQuantity(BigDecimal.valueOf(100.0));
        stockRepository.saveAndFlush(bepStock);

        // Tạo đơn
        OrderDto.CreateCounterOrderRequest orderReq = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(2) // 2 ly * 20 = 40.0 đơn vị
                        .build()))
                .build();

        String orderRespStr = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderRespStr).path("data").path("orderId").asLong();

        mockMvc.perform(post("/api/v1/cashier/orders/" + orderId + "/confirm")
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/start")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        // 1. Hoàn thành lần đầu -> thành công
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/complete")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        Order completedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.HOAN_THANH);

        // Kiểm tra tồn kho BEP bị trừ đúng 40.0: 100.0 - 40.0 = 60.0
        Stock bepAfter = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP)).orElseThrow();
        assertThat(bepAfter.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(60.0));

        long movementsCountAfterFirst = movementRepository.findByMaterialIdAndLocationOrderByCreatedAtDesc(traDen.getId(), StockLocation.BEP).size();

        // 2. Hoàn thành lần thứ hai (Idempotent) -> không ném lỗi và KHÔNG trừ thêm kho
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/complete")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        Stock bepAfterSecond = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP)).orElseThrow();
        assertThat(bepAfterSecond.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(60.0));

        long movementsCountAfterSecond = movementRepository.findByMaterialIdAndLocationOrderByCreatedAtDesc(traDen.getId(), StockLocation.BEP).size();
        assertThat(movementsCountAfterSecond).isEqualTo(movementsCountAfterFirst);
    }

    @Test
    @DisplayName("5. Hủy đơn trước pha: Không tự động hoàn/cộng lại nguyên liệu vào kho")
    void testCancelOrderBeforeCookingDoesNotRestoreInventory() throws Exception {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        Material traDen = materialRepository.findAll().stream().findFirst().orElseThrow();

        Stock bepStock = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP))
                .orElseGet(() -> Stock.builder()
                        .id(new StockId(traDen.getId(), StockLocation.BEP))
                        .material(traDen)
                        .threshold(BigDecimal.ZERO)
                        .build());
        bepStock.setQuantity(BigDecimal.valueOf(50.0));
        stockRepository.save(bepStock);

        // Tạo đơn
        OrderDto.CreateCounterOrderRequest orderReq = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        String orderRespStr = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderRespStr).path("data").path("orderId").asLong();

        // Cashier confirm -> Đơn ở CHO_CHE_BIEN
        mockMvc.perform(post("/api/v1/cashier/orders/" + orderId + "/confirm")
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk());

        BigDecimal stockBeforeCancel = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP))
                .orElseThrow().getQuantity();

        // Khách gửi yêu cầu hủy trước pha
        CancellationDto.CreateCancellationRequest cancelReq = CancellationDto.CreateCancellationRequest.builder()
                .reason("Khách đổi ý muốn hủy đơn")
                .build();

        String cancelRespStr = mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancellation-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cancelRequestId = objectMapper.readTree(cancelRespStr).path("data").path("id").asLong();

        // Thu ngân duyệt hủy đơn
        CancellationDto.DecideCancellationRequest decisionReq = CancellationDto.DecideCancellationRequest.builder()
                .approved(true)
                .reason("Đồng ý hủy trước khi nấu")
                .build();

        mockMvc.perform(post("/api/v1/cashier/cancellation-requests/" + cancelRequestId + "/decision")
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionReq)))
                .andExpect(status().isOk());

        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.DA_HUY);

        // Đảm bảo kho KHÔNG bị tự động cộng thêm nguyên liệu
        BigDecimal stockAfterCancel = stockRepository.findById(new StockId(traDen.getId(), StockLocation.BEP))
                .orElseThrow().getQuantity();
        assertThat(stockAfterCancel).isEqualByComparingTo(stockBeforeCancel);
    }

    @Test
    @DisplayName("6. Báo cáo tài chính phân biệt: Doanh số hoàn thành, Thực thu, Thực hoàn và Dư nợ chưa thu")
    void testCompletedUnpaidOrderAndFinancialReportDistinction() throws Exception {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        Material mat = materialRepository.findAll().stream().findFirst().orElseThrow();

        // Đảm bảo đủ tồn kho cho bếp
        Stock bepStock = stockRepository.findById(new StockId(mat.getId(), StockLocation.BEP))
                .orElseGet(() -> stockRepository.save(Stock.builder()
                        .id(new StockId(mat.getId(), StockLocation.BEP))
                        .material(mat)
                        .quantity(BigDecimal.valueOf(1000.0))
                        .threshold(BigDecimal.ZERO)
                        .build()));
        bepStock.setQuantity(bepStock.getQuantity().add(BigDecimal.valueOf(1000.0)));
        stockRepository.save(bepStock);

        // Đơn 1: Bếp HOÀN THÀNH nhưng CHƯA THANH TOÁN (Unpaid)
        OrderDto.CreateCounterOrderRequest o1Req = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        String o1Resp = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(o1Req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long o1Id = objectMapper.readTree(o1Resp).path("data").path("orderId").asLong();

        mockMvc.perform(post("/api/v1/cashier/orders/" + o1Id + "/confirm")
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + o1Id + "/start")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + o1Id + "/complete")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        // Đơn 2: Bếp HOÀN THÀNH và ĐÃ THANH TOÁN (Paid)
        OrderDto.CreateCounterOrderRequest o2Req = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        String o2Resp = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(o2Req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long o2Id = objectMapper.readTree(o2Resp).path("data").path("orderId").asLong();

        mockMvc.perform(post("/api/v1/cashier/orders/" + o2Id + "/confirm")
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + o2Id + "/start")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + o2Id + "/complete")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        PaymentDto.RecordPaymentRequest payReq = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.CASH)
                .reference("TIEN-MAT-QUAY")
                .build();
        mockMvc.perform(post("/api/v1/cashier/orders/" + o2Id + "/payments")
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        // Lấy báo cáo doanh thu qua /api/v1/admin/reports/revenue
        mockMvc.perform(get("/api/v1/admin/reports/revenue")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("VND"))
                // Doanh số hoàn thành phải bao gồm cả Đơn 1 và Đơn 2
                .andExpect(jsonPath("$.data.completedOrderValue", greaterThan(0.0)))
                // Tiền thực thu phải phản ánh đúng payment của Đơn 2
                .andExpect(jsonPath("$.data.received", greaterThan(0.0)))
                // Tiền còn phải thu (currentOutstanding) phải phản ánh hóa đơn chưa thu của Đơn 1
                .andExpect(jsonPath("$.data.currentOutstanding", greaterThan(0.0)));

        // Lấy báo cáo số đơn theo trạng thái qua /api/v1/admin/reports/orders
        mockMvc.perform(get("/api/v1/admin/reports/orders")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.HOAN_THANH", greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("7. Bếp ghi sự cố đơn hàng và tiêu hao pha bù (REMAKE) sau hoàn thành")
    void testKitchenIncidentAndAdditionalConsumptionRemake() throws Exception {
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();
        Material mat = materialRepository.findAll().stream().findFirst().orElseThrow();

        Stock bepStock = stockRepository.findById(new StockId(mat.getId(), StockLocation.BEP))
                .orElseGet(() -> stockRepository.save(Stock.builder()
                        .id(new StockId(mat.getId(), StockLocation.BEP))
                        .material(mat)
                        .quantity(BigDecimal.valueOf(500.0))
                        .threshold(BigDecimal.ZERO)
                        .build()));
        bepStock.setQuantity(bepStock.getQuantity().add(BigDecimal.valueOf(500.0)));
        stockRepository.save(bepStock);

        // Tạo đơn
        OrderDto.CreateCounterOrderRequest oReq = OrderDto.CreateCounterOrderRequest.builder()
                .items(List.of(CartDto.CartItemRequest.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        String oResp = mockMvc.perform(post("/api/v1/orders/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(oResp).path("data").path("orderId").asLong();

        mockMvc.perform(post("/api/v1/cashier/orders/" + orderId + "/confirm")
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/start")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/complete")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isOk());

        // 1. Ghi nhận sự cố đơn hàng qua /api/v1/kitchen/orders/{orderId}/incidents
        InventoryDto.IncidentRequest incidentReq = InventoryDto.IncidentRequest.builder()
                .reason("Khách va chạm làm đổ ly trà sữa vừa hoàn thành")
                .notes("Cần pha lại 1 ly bù cho khách")
                .build();

        String incResp = mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/incidents")
                        .header("Authorization", kitchenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incidentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andReturn().getResponse().getContentAsString();

        Long incidentId = objectMapper.readTree(incResp).path("data").path("id").asLong();

        // 2. Bếp ghi tiêu hao pha bù (REMAKE) qua /api/v1/kitchen/orders/{orderId}/additional-consumptions
        BigDecimal bepBeforeRemake = stockRepository.findById(new StockId(mat.getId(), StockLocation.BEP))
                .orElseThrow().getQuantity();

        InventoryDto.AdditionalConsumptionRequest remakeReq = InventoryDto.AdditionalConsumptionRequest.builder()
                .reason("Pha bù sự cố #" + incidentId)
                .items(List.of(
                        InventoryDto.AdditionalConsumptionItemRequest.builder()
                                .materialId(mat.getId())
                                .quantity(BigDecimal.valueOf(15.0))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/additional-consumptions")
                        .header("Authorization", kitchenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remakeReq)))
                .andExpect(status().isCreated());

        // Kiểm tra tồn BEP bị trừ tiếp 15.0 cho pha bù
        BigDecimal bepAfterRemake = stockRepository.findById(new StockId(mat.getId(), StockLocation.BEP))
                .orElseThrow().getQuantity();
        assertThat(bepAfterRemake).isEqualByComparingTo(bepBeforeRemake.subtract(BigDecimal.valueOf(15.0)));

        // 3. Giải quyết sự cố qua /api/v1/kitchen/orders/{orderId}/incidents/{incidentId}/resolve
        mockMvc.perform(post("/api/v1/kitchen/orders/" + orderId + "/incidents/" + incidentId + "/resolve")
                        .header("Authorization", kitchenToken))
                .andExpect(status().isCreated());
    }
}
