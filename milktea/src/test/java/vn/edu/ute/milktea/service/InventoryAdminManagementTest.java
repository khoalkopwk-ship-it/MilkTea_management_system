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
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InventoryAdminManagementTest {

    @Mock StockRepository stockRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock MaterialRepository materialRepository;
    @Mock AccountRepository accountRepository;
    @Mock ImportReceiptRepository importReceiptRepository;
    @Mock ImportReceiptItemRepository importReceiptItemRepository;
    @Mock PreparationRecipeRepository recipeRepository;
    @Mock PreparationRecipeItemRepository recipeItemRepository;
    @Mock PreparationBatchRepository batchRepository;
    @Mock PreparationBatchItemRepository batchItemRepository;
    @Mock BusinessAuditRepository auditRepository;
    @Mock RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks InventoryService inventoryService;

    private Account adminUser;
    private Material rawTea;
    private Material preparedTea;
    private PreparationRecipe teaRecipe;

    @BeforeEach
    void setUp() {
        adminUser = Account.builder()
                .id(1L)
                .email("admin@milktea.vn")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .build();

        rawTea = Material.builder()
                .id(10L)
                .name("Trà Đen Thô")
                .type(MaterialType.THO)
                .unit("kg")
                .active(true)
                .build();

        preparedTea = Material.builder()
                .id(20L)
                .name("Nước Cốt Trà Đen")
                .type(MaterialType.SOCHE)
                .unit("ml")
                .active(true)
                .build();

        teaRecipe = PreparationRecipe.builder()
                .id(100L)
                .name("Định mức Nước Cốt Trà Đen")
                .outputMaterial(preparedTea)
                .standardOutputQuantity(BigDecimal.valueOf(5000))
                .active(true)
                .version(1L)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(adminUser));
    }

    @Test
    @DisplayName("6. Xác nhận nhập kho: Tính giá nhập bình quân gia quyền theo đúng ví dụ đề bài (2kg * 50k + 1kg * 65k = 3kg * 55k)")
    void testRecordImportWeightedAverageCost() {
        // Tồn trước: 2 kg, giá TB 50.000 đ/kg
        Stock existingKhoStock = Stock.builder()
                .id(new StockId(10L, StockLocation.KHO))
                .material(rawTea)
                .quantity(BigDecimal.valueOf(2.0))
                .costPrice(BigDecimal.valueOf(50000.0))
                .threshold(BigDecimal.valueOf(5.0))
                .build();

        when(stockRepository.findByIdWithLock(10L, StockLocation.KHO))
                .thenReturn(Optional.of(existingKhoStock));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));

        // Phiếu nhập: 1 kg, đơn giá 65.000 đ/kg
        InventoryDto.CreateImportReceiptRequest request = InventoryDto.CreateImportReceiptRequest.builder()
                .reason("Nhập lô trà mới")
                .supplier("Đại lý Trà Bảo Lộc")
                .items(List.of(
                        InventoryDto.ImportItemRequest.builder()
                                .materialId(10L)
                                .quantity(BigDecimal.valueOf(1.0))
                                .unitPrice(BigDecimal.valueOf(65000.0))
                                .build()
                ))
                .build();

        // Mock lưu ImportReceipt
        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(inv -> {
            ImportReceipt r = inv.getArgument(0);
            r.setId(999L);
            return r;
        });

        // Thực hiện nhập kho
        InventoryDto.ImportReceiptResponse response = inventoryService.recordImport(request, adminUser);

        // Kiểm tra tồn kho mới = 3 kg
        assertEquals(0, BigDecimal.valueOf(3.0).compareTo(existingKhoStock.getQuantity()));

        // Kiểm tra giá TB mới = 55.000 đ/kg:
        // (2 * 50.000 + 1 * 65.000) / 3 = 165.000 / 3 = 55.000
        assertEquals(0, BigDecimal.valueOf(55000.0).compareTo(existingKhoStock.getCostPrice()));

        // Kiểm tra lưu biến động kho StockMovement với unitCost = 65.000
        verify(movementRepository).saveAll(argThat(iterable -> {
            List<StockMovement> list = (List<StockMovement>) iterable;
            if (list.isEmpty()) return false;
            StockMovement m = list.get(0);
            return m.getType() == StockMovementType.NHAP &&
                    m.getLocation() == StockLocation.KHO &&
                    m.getUnitCost().compareTo(BigDecimal.valueOf(65000.0)) == 0 &&
                    m.getDeltaQuantity().compareTo(BigDecimal.valueOf(1.0)) == 0 &&
                    m.getAfterQuantity().compareTo(BigDecimal.valueOf(3.0)) == 0;
        }));

        // Kiểm tra phát sự kiện WebSocket STOCK_CHANGED after commit
        verify(realtimeEventPublisher, atLeastOnce()).publishAfterCommit(eq("/topic/stock"), eq("STOCK_CHANGED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("6. Xác nhận nhập kho khi tồn cũ = 0: Giá TB mới bằng đơn giá nhập mới")
    void testRecordImportWhenInitialStockIsZero() {
        Stock emptyStock = Stock.builder()
                .id(new StockId(10L, StockLocation.KHO))
                .material(rawTea)
                .quantity(BigDecimal.ZERO)
                .costPrice(BigDecimal.ZERO)
                .threshold(BigDecimal.valueOf(5.0))
                .build();

        when(stockRepository.findByIdWithLock(10L, StockLocation.KHO))
                .thenReturn(Optional.of(emptyStock));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));

        InventoryDto.CreateImportReceiptRequest request = InventoryDto.CreateImportReceiptRequest.builder()
                .reason("Nhập kho ban đầu")
                .items(List.of(
                        InventoryDto.ImportItemRequest.builder()
                                .materialId(10L)
                                .quantity(BigDecimal.valueOf(5.0))
                                .unitPrice(BigDecimal.valueOf(60000.0))
                                .build()
                ))
                .build();

        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(inv -> {
            ImportReceipt r = inv.getArgument(0);
            r.setId(1001L);
            return r;
        });

        inventoryService.recordImport(request, adminUser);

        assertEquals(0, BigDecimal.valueOf(5.0).compareTo(emptyStock.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(60000.0).compareTo(emptyStock.getCostPrice()));
    }

    @Test
    @DisplayName("4. Báo hao hụt kho ADMIN: Cố định Kho tổng KHO, không trừ Bếp, kiểm tra tồn âm")
    void testRecordWasteAtWarehouseAdminOnly() {
        Stock khoStock = Stock.builder()
                .id(new StockId(10L, StockLocation.KHO))
                .material(rawTea)
                .quantity(BigDecimal.valueOf(10.0))
                .costPrice(BigDecimal.valueOf(55000.0))
                .build();

        when(stockRepository.findByIdWithLock(10L, StockLocation.KHO))
                .thenReturn(Optional.of(khoStock));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));

        // Client cố tình gửi location = BEP
        InventoryDto.CreateWasteRequest request = InventoryDto.CreateWasteRequest.builder()
                .location(StockLocation.BEP)
                .reason("Bao bì rách, ẩm mốc")
                .items(List.of(
                        InventoryDto.WasteItemRequest.builder()
                                .materialId(10L)
                                .quantity(BigDecimal.valueOf(2.0))
                                .build()
                ))
                .build();

        inventoryService.recordWaste(request, adminUser);

        // Tồn Kho tổng giảm 2kg còn 8kg
        assertEquals(0, BigDecimal.valueOf(8.0).compareTo(khoStock.getQuantity()));

        // Lưu StockMovement với loại HAO_HUT tại KHO, giá vốn 55.000, chi phí tổn thất = 110.000
        verify(movementRepository).saveAll(argThat(iterable -> {
            List<StockMovement> list = (List<StockMovement>) iterable;
            if (list.isEmpty()) return false;
            StockMovement m = list.get(0);
            return m.getType() == StockMovementType.HAO_HUT &&
                    m.getLocation() == StockLocation.KHO &&
                    m.getUnitCost().compareTo(BigDecimal.valueOf(55000.0)) == 0 &&
                    m.getLossCost().compareTo(BigDecimal.valueOf(110000.0)) == 0 &&
                    "Bao bì rách, ẩm mốc".equals(m.getReason());
        }));

        // Kiểm tra ghi BusinessAudit
        verify(auditRepository).save(any(BusinessAudit.class));

        // Phát WebSocket STOCK_CHANGED
        verify(realtimeEventPublisher, atLeastOnce()).publishAfterCommit(eq("/topic/stock"), eq("STOCK_CHANGED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("4. Báo hao hụt kho ADMIN: Vượt quá số lượng tồn kho gây lỗi INVENTORY_INSUFFICIENT")
    void testRecordWasteInsufficientStockThrows() {
        Stock khoStock = Stock.builder()
                .id(new StockId(10L, StockLocation.KHO))
                .material(rawTea)
                .quantity(BigDecimal.valueOf(1.0))
                .costPrice(BigDecimal.valueOf(55000.0))
                .build();

        when(stockRepository.findByIdWithLock(10L, StockLocation.KHO))
                .thenReturn(Optional.of(khoStock));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));

        InventoryDto.CreateWasteRequest request = InventoryDto.CreateWasteRequest.builder()
                .location(StockLocation.KHO)
                .reason("Hao hụt quá mức")
                .items(List.of(
                        InventoryDto.WasteItemRequest.builder()
                                .materialId(10L)
                                .quantity(BigDecimal.valueOf(3.0)) // Cần hủy 3kg trong khi tồn chỉ có 1kg
                                .build()
                ))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                inventoryService.recordWaste(request, adminUser)
        );
        assertEquals(ErrorCode.INVENTORY_INSUFFICIENT, ex.getErrorCode());
    }

    @Test
    @DisplayName("5. Điều chỉnh nguyên liệu thô trong tab Tồn kho: Lưu audit, biến động DIEU_CHINH và lan truyền giá")
    void testAdjustRawMaterialStock() {
        Stock khoStock = Stock.builder()
                .id(new StockId(10L, StockLocation.KHO))
                .material(rawTea)
                .quantity(BigDecimal.valueOf(10.0))
                .costPrice(BigDecimal.valueOf(50000.0))
                .threshold(BigDecimal.valueOf(5.0))
                .build();

        when(stockRepository.findByIdWithLock(10L, StockLocation.KHO))
                .thenReturn(Optional.of(khoStock));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));

        // Điều chỉnh: tồn từ 10 -> 12, giá từ 50.000 -> 52.000, ngưỡng từ 5 -> 6, tên thay đổi
        InventoryDto.AdjustStockRequest request = InventoryDto.AdjustStockRequest.builder()
                .materialId(10L)
                .name("Trà Đen Thô Hảo Hạng")
                .quantity(BigDecimal.valueOf(12.0))
                .averageImportPrice(BigDecimal.valueOf(52000.0))
                .threshold(BigDecimal.valueOf(6.0))
                .reason("Kiểm kê thực tế thừa 2kg và điều chỉnh đơn giá theo hóa đơn bù")
                .build();

        // Mock công thức định mức phụ thuộc
        PreparationRecipeItem recipeItem = PreparationRecipeItem.builder()
                .id(new PreparationRecipeItemId(100L, 10L))
                .recipe(teaRecipe)
                .inputMaterial(rawTea)
                .standardInputQuantity(BigDecimal.valueOf(1.0))
                .build();
        when(recipeItemRepository.findByIdMaterialId(10L)).thenReturn(List.of(recipeItem));
        when(recipeItemRepository.findByIdRecipeId(100L)).thenReturn(List.of(recipeItem));

        inventoryService.adjustRawMaterialStock(request, adminUser);

        // Kiểm tra tồn kho mới = 12, giá vốn mới = 52.000, ngưỡng mới = 6
        assertEquals(0, BigDecimal.valueOf(12.0).compareTo(khoStock.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(52000.0).compareTo(khoStock.getCostPrice()));
        assertEquals(0, BigDecimal.valueOf(6.0).compareTo(khoStock.getThreshold()));
        assertEquals("Trà Đen Thô Hảo Hạng", rawTea.getName());

        // Kiểm tra lưu StockMovement loại DIEU_CHINH với delta = +2.0
        verify(movementRepository).save(argThat(movement ->
                movement.getType() == StockMovementType.DIEU_CHINH &&
                movement.getDeltaQuantity().compareTo(BigDecimal.valueOf(2.0)) == 0 &&
                movement.getLocation() == StockLocation.KHO
        ));

        // Kiểm tra lưu BusinessAudit
        verify(auditRepository).save(any(BusinessAudit.class));

        // Kiểm tra phát sự kiện WebSocket STOCK_CHANGED, MATERIAL_CHANGED, RECIPE_CHANGED
        verify(realtimeEventPublisher, atLeastOnce()).publishAfterCommit(eq("/topic/stock"), eq("STOCK_CHANGED"), any(), any(), any(), any());
        verify(realtimeEventPublisher, atLeastOnce()).publishAfterCommit(eq("/topic/admin"), eq("MATERIAL_CHANGED"), any(), any(), any(), any());
        verify(realtimeEventPublisher, atLeastOnce()).publishAfterCommit(eq("/topic/stock"), eq("RECIPE_CHANGED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("7. Phân biệt 3 mức giá vốn: Giá cấu hình hiện tại (11đ/ml), Giá mẻ (50k & 55k), Giá tồn Bếp bình quân (10.5đ/ml)")
    void testThreeTierCostPricingVerification() {
        // 1. Giá cấu hình thành phẩm sơ chế:
        // Định mức 1 kg trà thô -> 5000 ml nước cốt trà
        // Giá trà thô hiện tại = 55.000 đ/kg
        // Giá vốn cấu hình = 1 * 55.000 / 5000 = 11 đ/ml
        BigDecimal configuredUnitCost = BigDecimal.valueOf(55000.0)
                .divide(BigDecimal.valueOf(5000.0), 4, RoundingMode.HALF_UP);
        assertEquals(0, BigDecimal.valueOf(11.0).compareTo(configuredUnitCost));

        // 2. Mô phỏng Bếp ghi nhận mẻ:
        // Mẻ 1: 5000 ml với chi phí 50.000đ -> tồn Bếp ban đầu có 5000 ml, giá vốn 10.0 đ/ml
        Stock bepStock = Stock.builder()
                .id(new StockId(20L, StockLocation.BEP))
                .material(preparedTea)
                .quantity(BigDecimal.valueOf(5000.0))
                .costPrice(BigDecimal.valueOf(10.0))
                .threshold(BigDecimal.valueOf(1000.0))
                .build();
        when(stockRepository.findByIdWithLock(20L, StockLocation.BEP))
                .thenReturn(Optional.of(bepStock));

        // Nguyên liệu thô ở Bếp để sản xuất mẻ 2: 2kg trà thô giá 55.000 đ/kg
        Stock bepRawTeaStock = Stock.builder()
                .id(new StockId(10L, StockLocation.BEP))
                .material(rawTea)
                .quantity(BigDecimal.valueOf(2.0))
                .costPrice(BigDecimal.valueOf(55000.0))
                .threshold(BigDecimal.valueOf(1.0))
                .build();
        when(stockRepository.findByIdWithLock(10L, StockLocation.BEP))
                .thenReturn(Optional.of(bepRawTeaStock));
        when(stockRepository.findById(new StockId(10L, StockLocation.BEP)))
                .thenReturn(Optional.of(bepRawTeaStock));
        when(materialRepository.findById(10L)).thenReturn(Optional.of(rawTea));
        when(materialRepository.findById(20L)).thenReturn(Optional.of(preparedTea));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(adminUser));

        // Định mức sản xuất mẻ 2: 1kg trà thô -> 5000ml thành phẩm
        PreparationRecipeItem teaItem = PreparationRecipeItem.builder()
                .id(new PreparationRecipeItemId(100L, 10L))
                .recipe(teaRecipe)
                .inputMaterial(rawTea)
                .standardInputQuantity(BigDecimal.valueOf(1.0))
                .build();
        when(recipeItemRepository.findByIdRecipeId(100L)).thenReturn(List.of(teaItem));
        when(recipeRepository.findById(100L)).thenReturn(Optional.of(teaRecipe));
        when(batchRepository.save(any())).thenAnswer(inv -> {
            PreparationBatch b = inv.getArgument(0);
            b.setId(2001L);
            return b;
        });

        InventoryDto.CreatePreparationBatchRequest batchRequest = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(100L)
                .outputMaterialId(20L)
                .recipeVersion(1L)
                .desiredQuantity(BigDecimal.valueOf(5000.0))
                .actualQuantity(BigDecimal.valueOf(5000.0))
                .note("Nấu mẻ trà chiều")
                .inputItems(List.of(InventoryDto.BatchInputItemRequest.builder()
                        .materialId(10L)
                        .actualQuantity(BigDecimal.valueOf(1.0))
                        .unitPrice(BigDecimal.valueOf(55000.0))
                        .build()))
                .build();

        inventoryService.recordPreparation(batchRequest, adminUser, "KEY-BATCH-001");

        // Mẻ 2 có chi phí 55.000đ cho 5000ml (11đ/ml).
        // Tồn Bếp trước: 5000ml @ 10đ/ml (50.000đ).
        // Mẻ mới: 5000ml @ 11đ/ml (55.000đ).
        // Tồn Bếp mới: 10.000ml.
        // Giá vốn tồn Bếp mới = (50.000 + 55.000) / 10.000 = 105.000 / 10.000 = 10.5 đ/ml!
        assertEquals(0, BigDecimal.valueOf(10000.0).compareTo(bepStock.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(10.5).compareTo(bepStock.getCostPrice()));

        // Kiểm tra hao hụt 100 ml theo giá vốn tồn Bếp (10.5 đ/ml):
        // Chi phí hao hụt = 100 * 10.5 = 1.050 đồng
        BigDecimal wasteAmount = BigDecimal.valueOf(100.0);
        BigDecimal wasteLossCost = wasteAmount.multiply(bepStock.getCostPrice()).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, BigDecimal.valueOf(1050.0).compareTo(wasteLossCost));

        // Và giá vốn cấu hình hiện tại vẫn độc lập là 11.0 đ/ml
        assertEquals(0, BigDecimal.valueOf(11.0).compareTo(configuredUnitCost));
    }
}
