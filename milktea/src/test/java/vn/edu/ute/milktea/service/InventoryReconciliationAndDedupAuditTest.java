package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.entity.branch.Branch;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.branch.BranchRepository;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.service.branch.BranchService;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryReconciliationAndDedupAuditTest {

    @Mock private StockRepository stockRepository;
    @Mock private StockMovementRepository movementRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PreparationBatchRepository batchRepository;
    @Mock private ImportReceiptRepository importReceiptRepository;
    @Mock private ImportReceiptItemRepository importReceiptItemRepository;
    @Mock private ShiftSettlementRepository shiftSettlementRepository;
    @Mock private ShiftSettlementItemRepository shiftSettlementItemRepository;
    @Mock private BusinessAuditRepository auditRepository;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;
    @Mock private BranchService branchService;
    @Mock private BranchRepository branchRepository;

    private InventoryService inventoryService;

    private Branch centralWarehouse;
    private Account adminUser;

    @BeforeEach
    void setUp() {
        centralWarehouse = Branch.builder().id(99L).code("KHOTONG").name("Kho Tổng Trung Tâm").status("HOAT_DONG").build();
        adminUser = Account.builder().id(1L).fullName("Quản trị Kho").role(Role.ADMIN).build();

        inventoryService = new InventoryService(
                stockRepository, movementRepository, materialRepository, accountRepository,
                batchRepository, null, null, null, null, null, null,
                importReceiptRepository, importReceiptItemRepository,
                shiftSettlementRepository, shiftSettlementItemRepository,
                auditRepository, realtimeEventPublisher, branchService, branchRepository
        );
    }

    @Test
    @DisplayName("F05 & F21: Nhập kho trả về kết quả cũ và không ghi sổ lại nếu gửi trùng MaYeuCau / idempotencyKey")
    void testRecordImportDuplicateIdempotencyKeyReturnsExisting() {
        when(importReceiptRepository.findByIdempotencyKey("KEY-IMPORT-DUPLICATE-01"))
                .thenReturn(Optional.of(ImportReceipt.builder()
                        .id(123L)
                        .supplier("Công ty Trà Toàn Cầu")
                        .status(DocumentStatus.DA_GHI_SO)
                        .build()));

        InventoryDto.CreateImportReceiptRequest req = InventoryDto.CreateImportReceiptRequest.builder()
                .supplier("Công ty Trà Toàn Cầu")
                .items(List.of(InventoryDto.ImportItemRequest.builder()
                        .materialId(10L)
                        .quantity(BigDecimal.valueOf(50))
                        .unitPrice(BigDecimal.valueOf(100000))
                        .build()))
                .build();

        var res = inventoryService.recordImport(req, adminUser, null, "KEY-IMPORT-DUPLICATE-01");

        assertNotNull(res);
        assertEquals(123L, res.getId());
        verify(movementRepository, never()).saveAll(any());
        verify(importReceiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("F19: Kết ca kiểm kê khi có chênh lệch ghi nhận cả điều chỉnh DIEU_CHINH và hao hụt HAO_HUT")
    void testShiftSettlementRecordsBothAdjustmentAndWaste() {
        Material tea = Material.builder().id(5L).name("Cốt Trà Nhài").unit("ml").type(MaterialType.SOCHE).build();
        when(materialRepository.findById(5L)).thenReturn(Optional.of(tea));

        Stock stock = Stock.builder()
                .id(new StockId(99L, 5L, StockLocation.BEP))
                .quantity(BigDecimal.valueOf(5000))
                .costPrice(BigDecimal.valueOf(12))
                .build();
        when(stockRepository.findByIdWithLock(99L, 5L, StockLocation.BEP)).thenReturn(Optional.of(stock));
        when(branchService.getOrCreateCentralWarehouse()).thenReturn(centralWarehouse);
        when(branchRepository.findById(99L)).thenReturn(Optional.of(centralWarehouse));
        when(stockRepository.findById(new StockId(99L, 5L, StockLocation.KHO))).thenReturn(Optional.of(stock));
        when(shiftSettlementItemRepository.findBySettlementId(any())).thenReturn(List.of());

        when(shiftSettlementRepository.findByIdempotencyKey("SETTLE-F19")).thenReturn(Optional.empty());
        when(shiftSettlementRepository.save(any())).thenAnswer(inv -> {
            ShiftSettlement s = inv.getArgument(0);
            s.setId(88L);
            return s;
        });

        // Tồn sổ sách là 5000. Thực tế đo được 4800 (lệch -200). Trong 4800 đó, loại bỏ 300, giữ lại 4500.
        InventoryDto.CreateShiftSettlementRequest req = InventoryDto.CreateShiftSettlementRequest.builder()
                .branchId(99L)
                .location(StockLocation.BEP)
                .note("Kiểm kê cuối ngày F19")
                .items(List.of(InventoryDto.ShiftSettlementItemRequest.builder()
                        .materialId(5L)
                        .actualQuantity(BigDecimal.valueOf(4800))
                        .discardedQuantity(BigDecimal.valueOf(300))
                        .retainedQuantity(BigDecimal.valueOf(4500))
                        .reason("Hao hụt tự nhiên và trà quá giờ")
                        .build()))
                .build();

        inventoryService.recordShiftSettlement(req, adminUser, "SETTLE-F19");

        ArgumentCaptor<List<StockMovement>> captor = ArgumentCaptor.forClass(List.class);
        verify(movementRepository).saveAll(captor.capture());

        List<StockMovement> savedMovements = captor.getValue();
        assertEquals(2, savedMovements.size(), "Phải ghi nhận đủ 2 bước: điều chỉnh chênh lệch và xuất hao hụt");

        StockMovement adj = savedMovements.stream().filter(m -> m.getType() == StockMovementType.DIEU_CHINH).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-200"), adj.getDeltaQuantity());

        StockMovement waste = savedMovements.stream().filter(m -> m.getType() == StockMovementType.HAO_HUT).findFirst().orElseThrow();
        assertEquals(new BigDecimal("-300"), waste.getDeltaQuantity());
    }
}
