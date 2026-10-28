package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.controller.AdminController;
import vn.edu.ute.milktea.dto.AdminDto;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchProductRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.branch.StaffBranchRepository;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.repository.order.OrderIngredientSnapshotRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.repository.table.DiningTableRepository;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.admin.AdminService;
import vn.edu.ute.milktea.service.branch.BranchService;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CentralWarehouseAndAdminRoleTest {

    @Mock StockRepository stockRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock MaterialRepository materialRepository;
    @Mock AccountRepository accountRepository;
    @Mock PreparationBatchRepository batchRepository;
    @Mock PreparationBatchItemRepository batchItemRepository;
    @Mock PreparationRecipeRepository preparationRecipeRepository;
    @Mock PreparationRecipeItemRepository preparationRecipeItemRepository;
    @Mock OrderIngredientSnapshotRepository snapshotRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRecipeRepository productRecipeRepository;
    @Mock ImportReceiptRepository importReceiptRepository;
    @Mock ImportReceiptItemRepository importReceiptItemRepository;
    @Mock ShiftSettlementRepository shiftSettlementRepository;
    @Mock ShiftSettlementItemRepository shiftSettlementItemRepository;
    @Mock BusinessAuditRepository auditRepository;
    @Mock RealtimeEventPublisher realtimeEventPublisher;
    @Mock BranchService branchService;
    @Mock BranchRepository branchRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductRepository productRepository;
    @Mock DiningTableRepository tableRepository;
    @Mock StaffBranchRepository staffBranchRepository;
    @Mock BranchProductRepository branchProductRepository;
    @Mock PasswordEncoder passwordEncoder;

    private InventoryService inventoryService;
    private AdminService adminService;
    private AdminController adminController;

    private Branch centralWarehouse;
    private Branch branch1;
    private Material rawMaterial;
    private Account adminUser;
    private Account managerUser;
    private CurrentActor managerActor;
    private CurrentActor adminActor;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                stockRepository,
                movementRepository,
                materialRepository,
                accountRepository,
                batchRepository,
                batchItemRepository,
                preparationRecipeRepository,
                preparationRecipeItemRepository,
                snapshotRepository,
                orderItemRepository,
                productRecipeRepository,
                importReceiptRepository,
                importReceiptItemRepository,
                shiftSettlementRepository,
                shiftSettlementItemRepository,
                auditRepository,
                realtimeEventPublisher,
                branchService,
                branchRepository
        );

        adminService = new AdminService(
                accountRepository,
                categoryRepository,
                productRepository,
                productRecipeRepository,
                tableRepository,
                materialRepository,
                preparationRecipeRepository,
                preparationRecipeItemRepository,
                importReceiptItemRepository,
                stockRepository,
                auditRepository,
                realtimeEventPublisher,
                passwordEncoder,
                branchRepository,
                staffBranchRepository,
                branchProductRepository,
                branchService
        );

        adminController = new AdminController(adminService, inventoryService, null, null, branchService);

        centralWarehouse = Branch.builder()
                .id(100L)
                .code("KHOTONG")
                .name("Kho Hàng Tổng (Toàn Chuỗi)")
                .status("HOAT_DONG")
                .build();

        branch1 = Branch.builder()
                .id(1L)
                .code("CN1")
                .name("Chi nhánh Quận 1")
                .status("HOAT_DONG")
                .build();

        rawMaterial = Material.builder()
                .id(10L)
                .name("Trà Đen")
                .unit("kg")
                .type(MaterialType.THO)
                .active(true)
                .build();

        adminUser = Account.builder()
                .id(1L)
                .fullName("Quản trị viên")
                .email("admin@milktea.vn")
                .role(Role.ADMIN)
                .active(true)
                .build();

        managerUser = Account.builder()
                .id(2L)
                .fullName("Quản lý CN1")
                .email("manager@milktea.vn")
                .role(Role.MANAGER)
                .active(true)
                .build();

        adminActor = CurrentActor.builder()
                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                .accountId(1L)
                .email("admin@milktea.vn")
                .role(Role.ADMIN)
                .build();

        managerActor = CurrentActor.builder()
                .actorType(CurrentActor.ActorType.AUTHENTICATED)
                .accountId(2L)
                .email("manager@milktea.vn")
                .role(Role.MANAGER)
                .branchId(1L)
                .build();

        when(branchService.getOrCreateCentralWarehouse()).thenReturn(centralWarehouse);
        when(branchRepository.findById(100L)).thenReturn(Optional.of(centralWarehouse));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawMaterial));
    }

    @Test
    @DisplayName("Admin import to Central Warehouse (KHOTONG) updates stock, weighted average cost price, and records movement")
    void testAdminImportSupplierToCentralWarehouse() {
        Stock centralStock = Stock.builder()
                .id(new StockId(100L, 10L, StockLocation.KHO))
                .branch(centralWarehouse)
                .material(rawMaterial)
                .quantity(new BigDecimal("20.00"))
                .costPrice(new BigDecimal("40000.00"))
                .threshold(new BigDecimal("5.00"))
                .build();

        when(stockRepository.findByIdWithLock(eq(100L), eq(10L), eq(StockLocation.KHO)))
                .thenReturn(Optional.of(centralStock));
        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(inv -> {
            ImportReceipt r = inv.getArgument(0);
            r.setId(501L);
            return r;
        });

        InventoryDto.CreateImportReceiptRequest req = InventoryDto.CreateImportReceiptRequest.builder()
                .supplier("Công ty Trà Bảo Lộc")
                .reason("Nhập nguyên liệu tổng")
                .branchId(100L)
                .items(List.of(
                        InventoryDto.ImportItemRequest.builder()
                                .materialId(10L)
                                .quantity(new BigDecimal("30.00"))
                                .unitPrice(new BigDecimal("60000.00"))
                                .build()
                ))
                .build();

        InventoryDto.ImportReceiptResponse resp = inventoryService.recordImport(req, adminUser, 100L);

        assertNotNull(resp);
        assertEquals(100L, resp.getBranchId());
        // New Qty = 20 + 30 = 50.00
        assertEquals(0, new BigDecimal("50.00").compareTo(centralStock.getQuantity()));
        // Weighted average cost: (20 * 40,000 + 30 * 60,000) / 50 = (800,000 + 1,800,000) / 50 = 2,600,000 / 50 = 52,000.00
        assertEquals(0, new BigDecimal("52000.00").compareTo(centralStock.getCostPrice()));

        verify(movementRepository).saveAll(argThat(movements -> {
            List<StockMovement> list = (List<StockMovement>) movements;
            return list.size() == 1 && list.get(0).getType() == StockMovementType.NHAP;
        }));
    }

    @Test
    @DisplayName("Branch import draws from Central Warehouse: deducts KHOTONG, increases Branch stock, inherits cost price")
    void testBranchImportFromCentralWarehouseSuccess() {
        Stock centralStock = Stock.builder()
                .id(new StockId(100L, 10L, StockLocation.KHO))
                .branch(centralWarehouse)
                .material(rawMaterial)
                .quantity(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("52000.00"))
                .threshold(new BigDecimal("5.00"))
                .build();

        Stock branchStock = Stock.builder()
                .id(new StockId(1L, 10L, StockLocation.KHO))
                .branch(branch1)
                .material(rawMaterial)
                .quantity(new BigDecimal("5.00"))
                .costPrice(new BigDecimal("50000.00"))
                .threshold(new BigDecimal("2.00"))
                .build();

        when(stockRepository.findByIdWithLock(eq(100L), eq(10L), eq(StockLocation.KHO)))
                .thenReturn(Optional.of(centralStock));
        when(stockRepository.findByIdWithLock(eq(1L), eq(10L), eq(StockLocation.KHO)))
                .thenReturn(Optional.of(branchStock));
        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(inv -> {
            ImportReceipt r = inv.getArgument(0);
            r.setId(502L);
            return r;
        });

        InventoryDto.CreateImportReceiptRequest req = InventoryDto.CreateImportReceiptRequest.builder()
                .supplier("Kho Hàng Tổng")
                .reason("Chuyển hàng từ Kho Tổng về Chi nhánh")
                .branchId(1L)
                .items(List.of(
                        InventoryDto.ImportItemRequest.builder()
                                .materialId(10L)
                                .quantity(new BigDecimal("15.00"))
                                .build()
                ))
                .build();

        InventoryDto.ImportReceiptResponse resp = inventoryService.recordImport(req, managerUser, 1L);

        assertNotNull(resp);
        assertEquals(1L, resp.getBranchId());

        // 1. Central warehouse decreased by 15: 50 - 15 = 35.00
        assertEquals(0, new BigDecimal("35.00").compareTo(centralStock.getQuantity()));

        // 2. Branch stock increased by 15: 5 + 15 = 20.00 and inherited unit cost from central: 52000.00
        assertEquals(0, new BigDecimal("20.00").compareTo(branchStock.getQuantity()));
        assertEquals(0, new BigDecimal("52000.00").compareTo(branchStock.getCostPrice()));

        // 3. Both movements verified: XUAT_CHUYEN for central, NHAP for branch
        verify(movementRepository).saveAll(argThat(movements -> {
            List<StockMovement> list = (List<StockMovement>) movements;
            boolean hasXuatChuyen = list.stream().anyMatch(m -> m.getType() == StockMovementType.XUAT_CHUYEN && m.getBranch().getId().equals(100L));
            boolean hasNhap = list.stream().anyMatch(m -> m.getType() == StockMovementType.NHAP && m.getBranch().getId().equals(1L));
            return hasXuatChuyen && hasNhap;
        }));
    }

    @Test
    @DisplayName("Branch import fails if Central Warehouse has insufficient stock with explicit message")
    void testBranchImportInsufficientCentralStock() {
        Stock centralStock = Stock.builder()
                .id(new StockId(100L, 10L, StockLocation.KHO))
                .branch(centralWarehouse)
                .material(rawMaterial)
                .quantity(new BigDecimal("8.00"))
                .costPrice(new BigDecimal("50000.00"))
                .build();

        when(stockRepository.findByIdWithLock(eq(100L), eq(10L), eq(StockLocation.KHO)))
                .thenReturn(Optional.of(centralStock));
        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(inv -> {
            ImportReceipt r = inv.getArgument(0);
            r.setId(503L);
            return r;
        });

        InventoryDto.CreateImportReceiptRequest req = InventoryDto.CreateImportReceiptRequest.builder()
                .supplier("Kho Hàng Tổng")
                .reason("Cung cấp chi nhánh")
                .branchId(1L)
                .items(List.of(
                        InventoryDto.ImportItemRequest.builder()
                                .materialId(10L)
                                .quantity(new BigDecimal("20.00"))
                                .build()
                ))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                inventoryService.recordImport(req, managerUser, 1L)
        );

        assertEquals(ErrorCode.INVENTORY_INSUFFICIENT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Kho hàng tổng không đủ nguyên liệu"));
        assertTrue(ex.getMessage().contains("Còn tồn: 8.00 kg"));
        assertTrue(ex.getMessage().contains("chi nhánh yêu cầu: 20.00 kg"));
    }

    @Test
    @DisplayName("Manager cannot edit averageImportPrice or material name on branch stock adjustment")
    void testManagerCannotEditCostPriceOrName() {
        Stock branchStock = Stock.builder()
                .id(new StockId(1L, 10L, StockLocation.KHO))
                .branch(branch1)
                .material(rawMaterial)
                .quantity(new BigDecimal("10.00"))
                .build();
        when(stockRepository.findByIdWithLock(eq(1L), eq(10L), eq(StockLocation.KHO)))
                .thenReturn(Optional.of(branchStock));

        InventoryDto.AdjustStockRequest reqPrice = InventoryDto.AdjustStockRequest.builder()
                .materialId(10L)
                .quantity(new BigDecimal("12.00"))
                .averageImportPrice(new BigDecimal("60000.00"))
                .reason("Sửa giá vốn")
                .build();

        BusinessException exPrice = assertThrows(BusinessException.class, () ->
                inventoryService.adjustRawMaterialStock(reqPrice, managerUser, 1L)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, exPrice.getErrorCode());

        InventoryDto.AdjustStockRequest reqName = InventoryDto.AdjustStockRequest.builder()
                .materialId(10L)
                .name("Trà Đen Đổi Tên")
                .quantity(new BigDecimal("12.00"))
                .reason("Đổi tên")
                .build();

        BusinessException exName = assertThrows(BusinessException.class, () ->
                inventoryService.adjustRawMaterialStock(reqName, managerUser, 1L)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, exName.getErrorCode());
        assertTrue(exName.getMessage().contains("đổi tên"));
    }

    @Test
    @DisplayName("Manager role is blocked with 403 when calling deleteMaterial")
    void testManagerForbiddenFromDeletingMaterial() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminController.deleteMaterial(10L, managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Quản lý chi nhánh không có quyền xóa nguyên liệu"));
    }

    @Test
    @DisplayName("Manager role is blocked with 403 when calling preparation recipe configuration APIs")
    void testManagerForbiddenFromPreparationRecipeApis() {
        // 1. List
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminController.listPreparationRecipes(managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex1.getErrorCode());

        // 2. Get
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminController.getPreparationRecipe(1L, managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex2.getErrorCode());

        // 3. Create
        AdminDto.CreatePreparationRecipeRequest createReq = AdminDto.CreatePreparationRecipeRequest.builder()
                .outputMaterialId(20L)
                .standardOutputQuantity(new BigDecimal("1000"))
                .build();
        BusinessException ex3 = assertThrows(BusinessException.class, () ->
                adminController.createPreparationRecipe(createReq, managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex3.getErrorCode());

        // 4. Update
        BusinessException ex4 = assertThrows(BusinessException.class, () ->
                adminController.updatePreparationRecipe(1L, createReq, managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex4.getErrorCode());

        // 5. Delete
        BusinessException ex5 = assertThrows(BusinessException.class, () ->
                adminController.deletePreparationRecipe(1L, managerActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex5.getErrorCode());
    }

    @Test
    @DisplayName("Preparation recipe update calls flush() right after deleteByIdRecipeId to prevent composite key collision")
    void testUpdatePreparationRecipeFlushesItemRepository() {
        Material outMat = Material.builder().id(20L).name("Cốt Trà Đen").type(MaterialType.SOCHE).build();
        PreparationRecipe recipe = PreparationRecipe.builder()
                .id(5L)
                .outputMaterial(outMat)
                .standardOutputQuantity(new BigDecimal("5000.00"))
                .version(1L)
                .active(true)
                .build();

        when(preparationRecipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawMaterial));
        when(materialRepository.findById(20L)).thenReturn(Optional.of(outMat));

        AdminDto.CreatePreparationRecipeRequest req = AdminDto.CreatePreparationRecipeRequest.builder()
                .outputMaterialId(20L)
                .standardOutputQuantity(new BigDecimal("5000.00"))
                .note("Cập nhật định mức")
                .active(true)
                .inputItems(List.of(
                        AdminDto.PrepRecipeItemDto.builder()
                                .inputMaterialId(10L)
                                .standardQuantity(new BigDecimal("1.50"))
                                .build()
                ))
                .build();

        adminService.updatePreparationRecipe(5L, req);

        // Verify that deleteByIdRecipeId is executed first, followed by flush(), before saveAll
        org.mockito.InOrder inOrder = inOrder(preparationRecipeItemRepository);
        inOrder.verify(preparationRecipeItemRepository).deleteByIdRecipeId(5L);
        inOrder.verify(preparationRecipeItemRepository).flush();
        inOrder.verify(preparationRecipeItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Admin role is blocked with 403 when calling table and QR management APIs")
    void testAdminRoleForbiddenFromTableAndQrApis() {
        // 1. List
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                adminController.listTables(adminActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex1.getErrorCode());
        assertTrue(ex1.getMessage().contains("Quản trị viên toàn chuỗi không quản lý sơ đồ bàn và mã QR"));

        // 2. Get
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                adminController.getTable(1L, adminActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex2.getErrorCode());

        // 3. Create
        AdminDto.TableRequest tableReq = AdminDto.TableRequest.builder()
                .name("Bàn 99")
                .branchId(1L)
                .active(true)
                .build();
        BusinessException ex3 = assertThrows(BusinessException.class, () ->
                adminController.createTable(tableReq, adminActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex3.getErrorCode());

        // 4. Update
        BusinessException ex4 = assertThrows(BusinessException.class, () ->
                adminController.updateTable(1L, tableReq, adminActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex4.getErrorCode());

        // 5. Delete
        BusinessException ex5 = assertThrows(BusinessException.class, () ->
                adminController.deleteTable(1L, adminActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex5.getErrorCode());

        // 6. QR
        BusinessException ex6 = assertThrows(BusinessException.class, () ->
                adminController.getTableQr(1L, adminActor)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex6.getErrorCode());
    }
}
