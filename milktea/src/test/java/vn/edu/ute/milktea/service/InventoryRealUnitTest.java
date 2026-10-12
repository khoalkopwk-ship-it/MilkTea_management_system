package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderIngredientSnapshot;
import vn.edu.ute.milktea.entity.order.OrderIngredientSnapshotId;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItemId;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.repository.order.OrderIngredientSnapshotRepository;
import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;
import vn.edu.ute.milktea.util.UnitConverter;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InventoryRealUnitTest {

    @Mock StockRepository stockRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock MaterialRepository materialRepository;
    @Mock AccountRepository accountRepository;
    @Mock PreparationBatchRepository batchRepository;
    @Mock PreparationBatchItemRepository batchItemRepository;
    @Mock PreparationRecipeRepository recipeRepository;
    @Mock PreparationRecipeItemRepository recipeItemRepository;
    @Mock OrderIngredientSnapshotRepository snapshotRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRecipeRepository productRecipeRepository;
    @Mock ImportReceiptRepository importReceiptRepository;
    @Mock ImportReceiptItemRepository importReceiptItemRepository;
    @Mock ShiftSettlementRepository shiftSettlementRepository;
    @Mock ShiftSettlementItemRepository shiftSettlementItemRepository;
    @Mock BusinessAuditRepository auditRepository;
    @Mock OrderRepository orderRepository;
    @Mock RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks InventoryService inventoryService;

    private Account kitchenUser;
    private Material rawTea;
    private Material preparedTea;
    private Material sugar;
    private Material boba;
    private PreparationRecipe teaRecipe;

    @BeforeEach
    void setUp() {
        kitchenUser = Account.builder()
                .id(100L)
                .email("kitchen1@milktea.vn")
                .fullName("Kitchen Staff 1")
                .build();

        rawTea = Material.builder()
                .id(1L)
                .name("Trà Đen Thô")
                .type(MaterialType.THO)
                .unit("kg")
                .build();

        preparedTea = Material.builder()
                .id(2L)
                .name("Nước Cốt Trà Đen")
                .type(MaterialType.SOCHE)
                .unit("ml")
                .build();

        sugar = Material.builder()
                .id(3L)
                .name("Đường")
                .type(MaterialType.THO)
                .unit("kg")
                .build();

        boba = Material.builder()
                .id(4L)
                .name("Trân Châu Nấu")
                .type(MaterialType.SOCHE)
                .unit("g")
                .build();

        teaRecipe = PreparationRecipe.builder()
                .id(10L)
                .name("Ủ Nước Cốt Trà Đen")
                .outputMaterial(preparedTea)
                .standardOutputQuantity(new BigDecimal("3000"))
                .tolerancePercent(new BigDecimal("10.00"))
                .active(true)
                .build();

        lenient().when(accountRepository.findByIdWithLock(100L)).thenReturn(Optional.of(kitchenUser));
        lenient().when(accountRepository.findById(100L)).thenReturn(Optional.of(kitchenUser));
        lenient().when(batchRepository.save(any(PreparationBatch.class))).thenAnswer(i -> {
            PreparationBatch b = i.getArgument(0);
            if (b.getId() == null) b.setId(1001L);
            return b;
        });
    }

    /**
     * TEST 1: VÍ DỤ NGHIỆM THU BẮT BUỘC
     * Mẻ 1: Dùng 1 kg trà thô, thu 3000 ml
     * Mẻ 2: Dùng 1.3 kg trà thô, thu 3000 ml
     * Giá vốn trà thô: 50.000 đ/kg
     * Tổng lượng trà thô: 2.3 kg -> Tổng chi phí: 115.000 đ
     * Tổng thành phẩm: 6000 ml
     * Giá vốn bình quân: 115.000 / 6000 = 19.166666... đ/ml
     * Hao hụt 300 ml -> Chi phí hao hụt = 300 * (115.000 / 6000) = 5.750 đ
     */
    @Test
    void testAcceptanceExample_TwoBatches_WeightedAverage_And_WasteSettlement() {
        // Setup recipe item
        PreparationRecipeItem item1 = PreparationRecipeItem.builder()
                .id(new PreparationRecipeItemId(10L, 1L))
                .recipe(teaRecipe)
                .inputMaterial(rawTea)
                .standardInputQuantity(new BigDecimal("1"))
                .build();
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(teaRecipe));
        when(recipeItemRepository.findByIdRecipeId(10L)).thenReturn(List.of(item1));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(rawTea));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(preparedTea));

        // Mock import price: 50,000 đ/kg for raw tea
        ImportReceiptItem importItem = ImportReceiptItem.builder()
                .quantity(new BigDecimal("10"))
                .unitPrice(new BigDecimal("50000"))
                .build();
        when(importReceiptItemRepository.findByIdMaterialId(1L)).thenReturn(List.of(importItem));

        // Stock in BEP
        Stock rawStock = Stock.builder()
                .id(new StockId(1L, StockLocation.BEP))
                .quantity(new BigDecimal("10.000"))
                .build();
        Stock prepStock = Stock.builder()
                .id(new StockId(2L, StockLocation.BEP))
                .quantity(BigDecimal.ZERO)
                .build();
        when(stockRepository.findByIdWithLock(1L, StockLocation.BEP)).thenReturn(Optional.of(rawStock));
        when(stockRepository.findByIdWithLock(2L, StockLocation.BEP)).thenReturn(Optional.of(prepStock));

        // 1. Record Mẻ 1: 3000 ml (chuẩn 1 kg @ 50,000 đ = 50,000 đ)
        InventoryDto.CreatePreparationBatchRequest req1 = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .desiredQuantity(new BigDecimal("3000"))
                .build();

        when(batchRepository.findByIdempotencyKey("BATCH-001")).thenReturn(Optional.empty());
        inventoryService.recordPreparation(req1, kitchenUser, "BATCH-001");

        // Verify Batch 1 snapshot cost: 1 * 50,000 = 50,000 đ. unitCost = 50,000 / 3000 = 16.666666...
        ArgumentCaptor<PreparationBatch> batchCaptor = ArgumentCaptor.forClass(PreparationBatch.class);
        verify(batchRepository).save(batchCaptor.capture());
        PreparationBatch savedBatch1 = batchCaptor.getValue();
        assertEquals(0, savedBatch1.getTotalCost().compareTo(new BigDecimal("50000.00")));
        assertEquals(0, savedBatch1.getActualQuantity().compareTo(new BigDecimal("3000")));

        // 2. Sau đó giá nhập nguyên liệu thô tăng lên 65,000 đ/kg
        ImportReceiptItem importItem2 = ImportReceiptItem.builder()
                .quantity(new BigDecimal("10"))
                .unitPrice(new BigDecimal("65000"))
                .build();
        when(importReceiptItemRepository.findByIdMaterialId(1L)).thenReturn(List.of(importItem2));

        // Record Mẻ 2: 3000 ml (chuẩn 1 kg @ 65,000 đ = 65,000 đ)
        InventoryDto.CreatePreparationBatchRequest req2 = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .desiredQuantity(new BigDecimal("3000"))
                .build();

        when(batchRepository.findByIdempotencyKey("BATCH-002")).thenReturn(Optional.empty());
        inventoryService.recordPreparation(req2, kitchenUser, "BATCH-002");

        verify(batchRepository, times(2)).save(batchCaptor.capture());
        PreparationBatch savedBatch2 = batchCaptor.getValue();
        assertEquals(0, savedBatch2.getTotalCost().compareTo(new BigDecimal("65000.00")));
        assertEquals(0, savedBatch2.getActualQuantity().compareTo(new BigDecimal("3000")));

        // 3. Test Weighted Average Unit Cost calculation
        savedBatch1.setUnitCost(new BigDecimal("50000").divide(new BigDecimal("3000"), 12, RoundingMode.HALF_UP));
        savedBatch2.setUnitCost(new BigDecimal("65000").divide(new BigDecimal("3000"), 12, RoundingMode.HALF_UP));
        savedBatch1.setStatus(DocumentStatus.DA_GHI_SO);
        savedBatch2.setStatus(DocumentStatus.DA_GHI_SO);
        when(batchRepository.findByOutputMaterialIdAndStatusAndPostedAtBetween(
                eq(2L), eq(DocumentStatus.DA_GHI_SO), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(savedBatch1, savedBatch2));

        Instant shiftStart = Instant.now().minusSeconds(3600);
        Instant shiftEnd = Instant.now();

        BigDecimal weightedAvgCost = inventoryService.calculateWeightedAverageUnitCost(
                2L, shiftStart, shiftEnd, BigDecimal.ZERO, BigDecimal.ZERO);

        assertNotNull(weightedAvgCost);
        // 115,000 / 6,000 = 19.166666666667
        BigDecimal expectedUnitCost = new BigDecimal("115000")
                .divide(new BigDecimal("6000"), 12, RoundingMode.HALF_UP);
        assertEquals(0, weightedAvgCost.compareTo(expectedUnitCost),
                "Weighted average unit cost must match 115000 / 6000 = 19.166666... đ/ml");
        assertTrue(weightedAvgCost.toString().startsWith("19.16666666666"));

        // 4. Test Shift Settlement & Hao hụt:
        // Bỏ 300 ml trà -> Chi phí hao hụt = 300 * (115.000 / 6000) = 5.750 đ
        BigDecimal wasteQty = new BigDecimal("300");
        BigDecimal calculatedWasteCost = wasteQty.multiply(weightedAvgCost).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, calculatedWasteCost.compareTo(new BigDecimal("5750.00")),
                "Waste cost for 300 ml discarded must be exactly 5,750 đ");

        // 5. Test Shift Settlement record creation
        when(shiftSettlementRepository.findByIdempotencyKey("SETTLE-CA1")).thenReturn(Optional.empty());
        Stock finalPrepStock = Stock.builder()
                .id(new StockId(2L, StockLocation.BEP))
                .quantity(new BigDecimal("6000"))
                .material(preparedTea)
                .build();
        when(stockRepository.findByIdWithLock(2L, StockLocation.BEP)).thenReturn(Optional.of(finalPrepStock));
        when(batchRepository.findByOutputMaterialIdAndStatus(2L, DocumentStatus.DA_GHI_SO))
                .thenReturn(List.of(savedBatch1, savedBatch2));
        when(shiftSettlementRepository.save(any(ShiftSettlement.class))).thenAnswer(i -> {
            ShiftSettlement s = i.getArgument(0);
            s.setId(555L);
            return s;
        });

        InventoryDto.CreateShiftSettlementRequest settleReq = InventoryDto.CreateShiftSettlementRequest.builder()
                .location(StockLocation.BEP)
                .note("Kết ca sáng")
                .items(List.of(InventoryDto.ShiftSettlementItemRequest.builder()
                        .materialId(2L)
                        .actualQuantity(new BigDecimal("5700"))
                        .discardedQuantity(new BigDecimal("300"))
                        .retainedQuantity(new BigDecimal("5400"))
                        .reason("Hết thời gian bảo quản cuối ca")
                        .build()))
                .build();

        var settleResp = inventoryService.recordShiftSettlement(settleReq, kitchenUser, "SETTLE-CA1");
        assertNotNull(settleResp);
        assertEquals(0, settleResp.getTotalLossCost().compareTo(new BigDecimal("5750.00")));
        // Only 300 ml discarded was subtracted from stock: 6000 - 300 = 5700
        assertEquals(0, finalPrepStock.getQuantity().compareTo(new BigDecimal("5700")));
    }

    /**
     * TEST 2: CHỨNG MINH BÌNH QUÂN GIA QUYỀN CÓ TRỌNG SỐ (Weighted Average vs Simple Arithmetic Average)
     * Mẻ A: Dùng 1 kg trà @ 50.000 -> Thu 2000 ml (Đơn giá = 25 đ/ml)
     * Mẻ B: Dùng 2 kg trà @ 50.000 -> Thu 5000 ml (Đơn giá = 20 đ/ml)
     * Trung bình cộng = (25 + 20) / 2 = 22.5 đ/ml
     * Bình quân gia quyền: (50.000 + 100.000) / (2000 + 5000) = 150.000 / 7000 = 21.4285714... đ/ml
     */
    @Test
    void testWeightedAverageVsSimpleArithmeticAverage() {
        PreparationBatch batchA = PreparationBatch.builder()
                .id(101L)
                .actualQuantity(new BigDecimal("2000"))
                .totalCost(new BigDecimal("50000"))
                .unitCost(new BigDecimal("25.000000000000"))
                .status(DocumentStatus.DA_GHI_SO)
                .build();

        PreparationBatch batchB = PreparationBatch.builder()
                .id(102L)
                .actualQuantity(new BigDecimal("5000"))
                .totalCost(new BigDecimal("100000"))
                .unitCost(new BigDecimal("20.000000000000"))
                .status(DocumentStatus.DA_GHI_SO)
                .build();

        when(batchRepository.findByOutputMaterialIdAndStatusAndPostedAtBetween(
                eq(2L), eq(DocumentStatus.DA_GHI_SO), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(batchA, batchB));

        BigDecimal result = inventoryService.calculateWeightedAverageUnitCost(
                2L, Instant.now().minusSeconds(3600), Instant.now(), BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal simpleAverage = new BigDecimal("22.5");
        BigDecimal expectedWeighted = new BigDecimal("150000").divide(new BigDecimal("7000"), 12, RoundingMode.HALF_UP);

        assertEquals(0, result.compareTo(expectedWeighted));
        assertNotEquals(0, result.compareTo(simpleAverage),
                "Must NOT use simple arithmetic average (22.5), must use weighted average (21.42857...)");
    }

    /**
     * TEST 3: MẺ NHIỀU NGUYÊN LIỆU ĐẦU VÀO VÀ TỒN ĐẦU CA
     * Đầu ca tồn: 1000 ml @ 22 đ/ml = 22.000 đ
     * Mẻ mới: 1 kg Trà thô @ 50.000 đ + 0.5 kg Đường @ 20.000 đ -> Thu 3000 ml
     * Chi phí mẻ = 50.000 + 10.000 = 60.000 đ
     * Tổng chi phí = 22.000 + 60.000 = 82.000 đ
     * Tổng sản lượng = 1000 + 3000 = 4000 ml
     * Giá vốn bình quân = 82.000 / 4000 = 20.5 đ/ml
     */
    @Test
    void testBatchMultipleInputs_AndInitialShiftStock() {
        PreparationBatch batchMulti = PreparationBatch.builder()
                .id(201L)
                .actualQuantity(new BigDecimal("3000"))
                .totalCost(new BigDecimal("60000"))
                .unitCost(new BigDecimal("20.000000000000"))
                .status(DocumentStatus.DA_GHI_SO)
                .build();

        when(batchRepository.findByOutputMaterialIdAndStatusAndPostedAtBetween(
                eq(2L), eq(DocumentStatus.DA_GHI_SO), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(batchMulti));

        BigDecimal initialQty = new BigDecimal("1000");
        BigDecimal initialCost = new BigDecimal("22000"); // 1000 ml * 22 đ/ml

        BigDecimal result = inventoryService.calculateWeightedAverageUnitCost(
                2L, Instant.now().minusSeconds(3600), Instant.now(), initialQty, initialCost);

        // Expected: (22000 + 60000) / (1000 + 3000) = 82000 / 4000 = 20.50 đ/ml
        BigDecimal expected = new BigDecimal("82000").divide(new BigDecimal("4000"), 12, RoundingMode.HALF_UP);
        assertEquals(0, result.compareTo(expected));
        assertEquals(0, result.compareTo(new BigDecimal("20.500000000000")));
    }

    /**
     * TEST 4: TỰ ĐỘNG TÍNH THEO ĐỊNH MỨC CỐ ĐỊNH VÀ PHÁT HIỆN LỆCH PHIÊN BẢN (VERSION CONFLICT)
     * Định mức chuẩn: 1 kg trà thô -> 5000 ml
     * Giá trà thô: 50.000 đ/kg
     * Case 4a: Bếp nhập mong muốn 2000 ml -> Backend tự tính cần 0.4 kg trà thô.
     *          Tổng chi phí mẻ: 0.4 * 50.000 = 20.000 đ.
     *          Giá vốn một đơn vị: 20.000 / 2000 = 10 đ/ml.
     * Case 4b: Admin đổi định mức lên version mới trong lúc Bếp mở form (version mismatch) -> Báo CONFLICT (ORDER_STATE_CONFLICT).
     * Case 4c: Định mức bị vô hiệu hóa (active = false) -> Bị chặn (VALIDATION_FAILED).
     */
    @Test
    void testFixedRecipeAutomaticCalculation_AndVersionConflict() {
        teaRecipe.setStandardOutputQuantity(new BigDecimal("5000"));
        teaRecipe.setVersion(1L);
        teaRecipe.setActive(true);

        PreparationRecipeItem item1 = PreparationRecipeItem.builder()
                .id(new PreparationRecipeItemId(10L, 1L))
                .recipe(teaRecipe)
                .inputMaterial(rawTea)
                .standardInputQuantity(new BigDecimal("1"))
                .build();
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(teaRecipe));
        when(recipeRepository.findByOutputMaterialIdAndActiveTrue(2L)).thenReturn(Optional.of(teaRecipe));
        when(recipeItemRepository.findByIdRecipeId(10L)).thenReturn(List.of(item1));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(rawTea));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(preparedTea));

        Stock rawStock = Stock.builder()
                .id(new StockId(1L, StockLocation.BEP))
                .quantity(new BigDecimal("100.000"))
                .build();
        Stock prepStock = Stock.builder()
                .id(new StockId(2L, StockLocation.BEP))
                .quantity(BigDecimal.ZERO)
                .build();
        when(stockRepository.findByIdWithLock(1L, StockLocation.BEP)).thenReturn(Optional.of(rawStock));
        when(stockRepository.findByIdWithLock(2L, StockLocation.BEP)).thenReturn(Optional.of(prepStock));
        when(importReceiptItemRepository.findByIdMaterialId(1L)).thenReturn(List.of(
                ImportReceiptItem.builder().quantity(BigDecimal.ONE).unitPrice(new BigDecimal("50000")).build()));

        // Case 4a: Bếp chỉ nhập mong muốn 2000 ml, không cần nhập đầu vào
        InventoryDto.CreatePreparationBatchRequest req1 = InventoryDto.CreatePreparationBatchRequest.builder()
                .outputMaterialId(2L)
                .recipeVersion(1L)
                .desiredQuantity(new BigDecimal("2000"))
                .build();
        when(batchRepository.findByIdempotencyKey("BATCH-AUTO-CALC")).thenReturn(Optional.empty());
        inventoryService.recordPreparation(req1, kitchenUser, "BATCH-AUTO-CALC");

        ArgumentCaptor<PreparationBatch> batchCaptor = ArgumentCaptor.forClass(PreparationBatch.class);
        verify(batchRepository).save(batchCaptor.capture());
        PreparationBatch saved = batchCaptor.getValue();

        // 2000 ml / 5000 ml * 1 kg = 0.4 kg trà thô -> 0.4 * 50.000 = 20.000 đ
        assertEquals(0, saved.getTotalCost().compareTo(new BigDecimal("20000.00")));
        assertEquals(0, saved.getActualQuantity().compareTo(new BigDecimal("2000")));
        assertEquals(0, saved.getUnitCost().compareTo(new BigDecimal("10.000000000000")));
        // Tồn Bếp của trà thô bị trừ đúng 0.4 kg: 100 - 0.4 = 99.6
        assertEquals(0, rawStock.getQuantity().compareTo(new BigDecimal("99.600")));

        // Case 4b: Version Conflict - Admin đã update định mức lên version 2
        teaRecipe.setVersion(2L);
        InventoryDto.CreatePreparationBatchRequest reqStaleVersion = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .recipeVersion(1L) // Gửi version 1 cũ
                .desiredQuantity(new BigDecimal("2000"))
                .build();
        BusinessException exConflict = assertThrows(BusinessException.class, () ->
                inventoryService.recordPreparation(reqStaleVersion, kitchenUser, "BATCH-STALE"));
        assertEquals(ErrorCode.ORDER_STATE_CONFLICT, exConflict.getErrorCode());

        // Case 4c: Định mức bị tắt (active = false)
        teaRecipe.setVersion(1L);
        teaRecipe.setActive(false);
        InventoryDto.CreatePreparationBatchRequest reqInactive = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .desiredQuantity(new BigDecimal("2000"))
                .build();
        BusinessException exInactive = assertThrows(BusinessException.class, () ->
                inventoryService.recordPreparation(reqInactive, kitchenUser, "BATCH-INACTIVE"));
        assertEquals(ErrorCode.VALIDATION_FAILED, exInactive.getErrorCode());
    }

    /**
     * TEST 5: HOÀN THÀNH ĐƠN TRỪ ĐÚNG ĐƠN VỊ THỰC TẾ THEO SIZE VÀ TOPPING
     * Order gồm: 2 ly Size M (150 ml/ly = 300 ml) + 1 ly Size L (200 ml) + 1 Topping Trân Châu (50 g)
     * Gộp: Nước Cốt Trà Đen = 500 ml, Trân Châu = 50 g
     * Trừ tại BEP, không trừ lại Trà Đen Thô tại KHO.
     */
    @Test
    void testOrderConsumption_RealUnits_SizeM_SizeL_Topping() {
        Order order = Order.builder().id(999L).build();

        // Snapshots created when order was placed
        OrderIngredientSnapshot snapTea = OrderIngredientSnapshot.builder()
                .id(new OrderIngredientSnapshotId(999L, 2L))
                .order(order)
                .material(preparedTea)
                .totalQuantity(new BigDecimal("500")) // 300 + 200 ml
                .snapshotType(MaterialType.SOCHE)
                .snapshotUnit("ml")
                .build();

        OrderIngredientSnapshot snapBoba = OrderIngredientSnapshot.builder()
                .id(new OrderIngredientSnapshotId(999L, 4L))
                .order(order)
                .material(boba)
                .totalQuantity(new BigDecimal("50")) // 50 g
                .snapshotType(MaterialType.SOCHE)
                .snapshotUnit("g")
                .build();

        when(snapshotRepository.findByIdOrderId(999L)).thenReturn(List.of(snapTea, snapBoba));

        Stock prepTeaStock = Stock.builder()
                .id(new StockId(2L, StockLocation.BEP))
                .quantity(new BigDecimal("1000")) // 1000 ml
                .build();

        Stock bobaStock = Stock.builder()
                .id(new StockId(4L, StockLocation.BEP))
                .quantity(new BigDecimal("200")) // 200 g
                .build();

        when(stockRepository.findByIdWithLock(2L, StockLocation.BEP)).thenReturn(Optional.of(prepTeaStock));
        when(stockRepository.findByIdWithLock(4L, StockLocation.BEP)).thenReturn(Optional.of(bobaStock));

        inventoryService.completeOrderConsumption(order, kitchenUser);

        // Verify stock deducted accurately
        assertEquals(0, prepTeaStock.getQuantity().compareTo(new BigDecimal("500")), "1000 - 500 = 500 ml");
        assertEquals(0, bobaStock.getQuantity().compareTo(new BigDecimal("150")), "200 - 50 = 150 g");

        // Verify movements recorded with delta
        verify(movementRepository).saveAll(argThat(m -> {
            var list = (List<StockMovement>) m;
            return list.size() == 2 &&
                    list.get(0).getDeltaQuantity().compareTo(new BigDecimal("-500")) == 0 &&
                    list.get(1).getDeltaQuantity().compareTo(new BigDecimal("-50")) == 0;
        }));

        // Verify raw tea stock was NEVER touched/looked up
        verify(stockRepository, never()).findByIdWithLock(1L, StockLocation.KHO);
    }

    /**
     * TEST 6: THIẾU TỒN KHO - BÁO LỖI CHI TIẾT VÀ ROLLBACK
     * Nhu cầu: 500 ml trà. Tồn BEP chỉ có 300 ml.
     * Ném BusinessException với thông báo rõ: tên nguyên liệu, cần bao nhiêu, còn bao nhiêu.
     */
    @Test
    void testInsufficientStockRollbackAndMessageDetails() {
        Order order = Order.builder().id(888L).build();
        OrderIngredientSnapshot snap = OrderIngredientSnapshot.builder()
                .id(new OrderIngredientSnapshotId(888L, 2L))
                .order(order)
                .material(preparedTea)
                .totalQuantity(new BigDecimal("500"))
                .snapshotType(MaterialType.SOCHE)
                .snapshotUnit("ml")
                .build();

        when(snapshotRepository.findByIdOrderId(888L)).thenReturn(List.of(snap));

        Stock lowStock = Stock.builder()
                .id(new StockId(2L, StockLocation.BEP))
                .quantity(new BigDecimal("300"))
                .build();
        when(stockRepository.findByIdWithLock(2L, StockLocation.BEP)).thenReturn(Optional.of(lowStock));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                inventoryService.completeOrderConsumption(order, kitchenUser));

        assertEquals(ErrorCode.INVENTORY_INSUFFICIENT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Nước Cốt Trà Đen"));
        assertTrue(ex.getMessage().contains("500"));
        assertTrue(ex.getMessage().contains("300"));

        // Verify no changes saved
        verify(stockRepository, never()).save(any());
        verify(movementRepository, never()).saveAll(any());
    }

    /**
     * TEST 7: KẾT CA VÀ HAO HỤT - PHÂN BIỆT LƯỢNG LOẠI BỎ VÀ LƯỢNG GIỮ LẠI
     * Tồn sổ: 1000 ml. Tồn thực tế: 950 ml.
     * Lượng loại bỏ: 300 ml. Lượng giữ lại: 650 ml.
     * Chỉ trừ tồn 300 ml bị loại bỏ (tồn còn 650 ml).
     * Chi phí hao hụt = 300 * 19.166666... = 5.750 đ.
     * Không trừ phần giữ lại, không tính hao hụt phần giữ lại.
     */
    @Test
    void testShiftSettlement_DiscardedVsRetainedQuantities_NoDoubleDeduction() {
        when(materialRepository.findById(2L)).thenReturn(Optional.of(preparedTea));
        when(shiftSettlementRepository.findByIdempotencyKey("SETTLE-CA-TOI")).thenReturn(Optional.empty());

        Stock prepStock = Stock.builder()
                .id(new StockId(2L, StockLocation.BEP))
                .quantity(new BigDecimal("950"))
                .material(preparedTea)
                .build();
        when(stockRepository.findByIdWithLock(2L, StockLocation.BEP)).thenReturn(Optional.of(prepStock));

        BigDecimal unitCost = new BigDecimal("115000").divide(new BigDecimal("6000"), 12, RoundingMode.HALF_UP);
        PreparationBatch b1 = PreparationBatch.builder()
                .actualQuantity(new BigDecimal("6000"))
                .totalCost(new BigDecimal("115000"))
                .status(DocumentStatus.DA_GHI_SO)
                .build();
        when(batchRepository.findByOutputMaterialIdAndStatus(2L, DocumentStatus.DA_GHI_SO))
                .thenReturn(List.of(b1));

        when(shiftSettlementRepository.save(any(ShiftSettlement.class))).thenAnswer(i -> {
            ShiftSettlement s = i.getArgument(0);
            s.setId(777L);
            return s;
        });

        InventoryDto.CreateShiftSettlementRequest req = InventoryDto.CreateShiftSettlementRequest.builder()
                .location(StockLocation.BEP)
                .note("Ca Tối")
                .items(List.of(InventoryDto.ShiftSettlementItemRequest.builder()
                        .materialId(2L)
                        .actualQuantity(new BigDecimal("950"))
                        .discardedQuantity(new BigDecimal("300"))
                        .retainedQuantity(new BigDecimal("650"))
                        .reason("Hết hạn cuối ca")
                        .build()))
                .build();

        var resp = inventoryService.recordShiftSettlement(req, kitchenUser, "SETTLE-CA-TOI");

        // Check total waste cost is only for 300 discarded: 5750.00
        assertEquals(0, resp.getTotalLossCost().compareTo(new BigDecimal("5750.00")));

        // Check stock after settlement is 650 (950 - 300 discarded = 650 retained)
        assertEquals(0, prepStock.getQuantity().compareTo(new BigDecimal("650")));

        // Check movement saved with delta -300 and type HAO_HUT
        verify(movementRepository).saveAll(argThat(m -> {
            var list = (List<StockMovement>) m;
            return list.size() == 1 &&
                    list.get(0).getDeltaQuantity().compareTo(new BigDecimal("-300")) == 0 &&
                    list.get(0).getType() == StockMovementType.HAO_HUT;
        }));
    }

    /**
     * TEST 8: VALIDATION CHẶN DỮ LIỆU KHÔNG HỢP LỆ VÀ SAI ĐƠN VỊ
     * - Sản lượng thực tế <= 0 -> VALIDATION_FAILED
     * - Lượng đầu vào <= 0 -> VALIDATION_FAILED
     * - Quy đổi khác thứ nguyên (kg <-> ml) bằng UnitConverter -> IllegalArgumentException
     */
    @Test
    void testInvalidInputsValidation() {
        // 8a. Sản lượng thực tế <= 0
        InventoryDto.CreatePreparationBatchRequest reqZeroYield = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .actualQuantity(BigDecimal.ZERO)
                .inputItems(List.of(InventoryDto.BatchInputItemRequest.builder()
                        .materialId(1L).actualQuantity(BigDecimal.ONE).unit("kg").build()))
                .build();

        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                inventoryService.recordPreparation(reqZeroYield, kitchenUser, "BATCH-ZERO"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex1.getErrorCode());

        // 8b. Định mức có sản lượng chuẩn đầu ra không hợp lệ (<= 0)
        teaRecipe.setStandardOutputQuantity(BigDecimal.ZERO);
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(teaRecipe));

        InventoryDto.CreatePreparationBatchRequest reqInvalidRecipe = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .desiredQuantity(new BigDecimal("3000"))
                .build();

        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                inventoryService.recordPreparation(reqInvalidRecipe, kitchenUser, "BATCH-INVALID-STD"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex2.getErrorCode());

        // 8c. Chặn quy đổi khối lượng sang thể tích bằng UnitConverter
        assertThrows(BusinessException.class, () ->
                UnitConverter.convert(new BigDecimal("1"), "kg", "ml"));

        assertThrows(BusinessException.class, () ->
                UnitConverter.convert(new BigDecimal("1000"), "ml", "g"));

        // Nhưng cùng thứ nguyên thì chuyển đổi chuẩn xác
        assertEquals(0, UnitConverter.convert(new BigDecimal("1"), "kg", "g").compareTo(new BigDecimal("1000")));
        assertEquals(0, UnitConverter.convert(new BigDecimal("500"), "g", "kg").compareTo(new BigDecimal("0.5")));
        assertEquals(0, UnitConverter.convert(new BigDecimal("1"), "l", "ml").compareTo(new BigDecimal("1000")));
        assertEquals(0, UnitConverter.convert(new BigDecimal("2500"), "ml", "l").compareTo(new BigDecimal("2.5")));
    }

    /**
     * TEST 9: CHỐNG GHI LẶP VÀ IDEMPOTENCY
     * - Trùng batch idempotency key -> không tạo mẻ mới
     * - Trùng settlement idempotency key -> không trừ kho lần hai
     */
    @Test
    void testIdempotency_DuplicateBatch_AndSettlement() {
        PreparationBatch existingBatch = PreparationBatch.builder()
                .id(888L)
                .idempotencyKey("KEY-DUP-BATCH")
                .createdBy(kitchenUser)
                .recipe(teaRecipe)
                .actualQuantity(new BigDecimal("3000"))
                .build();
        when(batchRepository.findByIdempotencyKey("KEY-DUP-BATCH")).thenReturn(Optional.of(existingBatch));
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(teaRecipe));

        InventoryDto.CreatePreparationBatchRequest dupBatch = InventoryDto.CreatePreparationBatchRequest.builder()
                .recipeId(10L)
                .actualQuantity(new BigDecimal("3000"))
                .build();

        // Idempotency: when key already exists, return silently without exception or duplicate insert
        inventoryService.recordPreparation(dupBatch, kitchenUser, "KEY-DUP-BATCH");
        verify(batchRepository, never()).save(any());

        ShiftSettlement existingSettle = ShiftSettlement.builder()
                .id(999L)
                .idempotencyKey("KEY-DUP-SETTLE")
                .location(StockLocation.BEP)
                .totalLossCost(BigDecimal.TEN)
                .build();
        when(shiftSettlementRepository.findByIdempotencyKey("KEY-DUP-SETTLE")).thenReturn(Optional.of(existingSettle));

        InventoryDto.CreateShiftSettlementRequest dupSettle = InventoryDto.CreateShiftSettlementRequest.builder()
                .location(StockLocation.BEP)
                .note("Ca Chiều")
                .items(List.of(InventoryDto.ShiftSettlementItemRequest.builder()
                        .materialId(2L)
                        .actualQuantity(new BigDecimal("1000"))
                        .discardedQuantity(new BigDecimal("100"))
                        .retainedQuantity(new BigDecimal("900"))
                        .build()))
                .build();

        var settleResult = inventoryService.recordShiftSettlement(dupSettle, kitchenUser, "KEY-DUP-SETTLE");
        assertNotNull(settleResult);
        assertEquals(999L, settleResult.getId());
        // Verify no duplicate save or stock manipulation
        verify(shiftSettlementRepository, never()).save(any());
        verify(stockRepository, never()).findByIdWithLock(any(), any());
    }
}
