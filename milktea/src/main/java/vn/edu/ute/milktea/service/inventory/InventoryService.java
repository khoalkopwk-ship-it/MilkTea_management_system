package vn.edu.ute.milktea.service.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderIngredientSnapshot;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.repository.order.OrderIngredientSnapshotRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeItemRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final MaterialRepository materialRepository;
    private final vn.edu.ute.milktea.repository.account.AccountRepository accountRepository;
    private final StockIssueRepository stockIssueRepository;
    private final StockIssueItemRepository stockIssueItemRepository;
    private final PreparationBatchRepository batchRepository;
    private final PreparationBatchItemRepository batchItemRepository;
    private final PreparationRecipeRepository recipeRepository;
    private final PreparationRecipeItemRepository recipeItemRepository;
    private final OrderIngredientSnapshotRepository snapshotRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ImportReceiptItemRepository importReceiptItemRepository;
    private final vn.edu.ute.milktea.repository.audit.BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.repository.order.OrderRepository orderRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public void completeOrderConsumption(Order order, Account kitchenAccount) {
        List<OrderIngredientSnapshot> snapshots = snapshotRepository.findByIdOrderId(order.getId());
        if (snapshots.isEmpty()) {
            return;
        }

        // Sắp xếp ID nguyên liệu tăng dần để tránh Deadlock khi khóa các dòng Stock
        List<Long> materialIds = snapshots.stream()
                .map(s -> s.getMaterial().getId())
                .distinct()
                .sorted()
                .toList();

        List<Stock> bepStocks = stockRepository.findByLocationAndMaterialIdInWithLock(StockLocation.BEP, materialIds);
        Map<Long, Stock> stockMap = new HashMap<>();
        bepStocks.forEach(s -> stockMap.put(s.getId().getMaterialId(), s));

        List<StockMovement> movements = new ArrayList<>();

        for (OrderIngredientSnapshot snap : snapshots) {
            Long matId = snap.getMaterial().getId();
            BigDecimal needed = snap.getTotalQuantity();

            Stock stock = stockMap.get(matId);
            if (stock == null || stock.getQuantity().compareTo(needed) < 0) {
                BigDecimal current = stock != null ? stock.getQuantity() : BigDecimal.ZERO;
                throw BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                        "Nguyên liệu [" + snap.getMaterial().getName() + "] tại Bếp không đủ tồn. Cần: "
                                + needed + " " + snap.getSnapshotUnit() + ", hiện có: " + current);
            }

            BigDecimal before = stock.getQuantity();
            BigDecimal after = before.subtract(needed);
            stock.setQuantity(after);

            StockMovement movement = StockMovement.builder()
                    .material(snap.getMaterial())
                    .location(StockLocation.BEP)
                    .type(StockMovementType.TIEU_HAO)
                    .deltaQuantity(needed.negate())
                    .beforeQuantity(before)
                    .afterQuantity(after)
                    .sourceType("ORDER")
                    .sourceId(order.getId())
                    .sourceLine(matId)
                    .createdBy(kitchenAccount)
                    .build();
            movements.add(movement);
        }

        stockRepository.saveAll(bepStocks);
        movementRepository.saveAll(movements);
    }

    @Transactional(readOnly = true)
    public java.util.List<InventoryDto.StockIssueResponse> listIssues() {
        return stockIssueRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(StockIssue::getCreatedAt).reversed())
                .map(i -> InventoryDto.StockIssueResponse.builder().id(i.getId()).createdAt(i.getCreatedAt())
                        .createdBy(i.getCreatedBy() != null ? i.getCreatedBy().getFullName() : "Nhân viên")
                        .items(stockIssueItemRepository.findByIdIssueId(i.getId()).stream().map(l ->
                                InventoryDto.StockIssueLineResponse.builder().materialName(l.getMaterial().getName())
                                        .unit(l.getMaterial().getUnit()).quantity(l.getQuantity()).build()).toList()).build()).toList();
    }

    @Transactional
    public void recordIssue(InventoryDto.CreateStockIssueRequest request, Account kitchenAccount) {
        recordIssue(request, kitchenAccount, null);
    }

    @Transactional
    public void recordIssue(InventoryDto.CreateStockIssueRequest request, Account kitchenAccount, String key) {
        if (kitchenAccount == null) throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chỉ nhân viên được lập phiếu");
        if (key != null && !key.isBlank()) {
            if (key.length() > 64) throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Mã yêu cầu quá dài");
            accountRepository.findByIdWithLock(kitchenAccount.getId()).orElseThrow();
            var existing = stockIssueRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                var old = existing.get();
                var lines = stockIssueItemRepository.findByIdIssueId(old.getId());
                boolean same = old.getCreatedBy().getId().equals(kitchenAccount.getId())
                        && java.util.Objects.equals(old.getReason(), request.getReason())
                        && lines.size() == request.getItems().size()
                        && lines.stream().allMatch(l -> request.getItems().stream().anyMatch(i ->
                                l.getMaterial().getId().equals(i.getMaterialId()) && l.getQuantity().compareTo(i.getQuantity()) == 0));
                if (!same) throw BusinessException.conflict(ErrorCode.IDEMPOTENCY_CONFLICT, "Mã yêu cầu đã dùng cho phiếu khác");
                return;
            }
        }
        StockIssue issue = StockIssue.builder()
                .idempotencyKey(key)
                .createdBy(kitchenAccount)
                .reason(request.getReason())
                .status(DocumentStatus.DA_GHI_SO)
                .createdAt(Instant.now())
                .postedAt(Instant.now())
                .build();
        issue = stockIssueRepository.save(issue);

        List<StockIssueItem> items = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (var itemReq : request.getItems().stream().sorted(java.util.Comparator.comparing(InventoryDto.StockIssueItemRequest::getMaterialId)).toList()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity().signum() <= 0 || !seen.add(itemReq.getMaterialId()))
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Số lượng phải dương và nguyên liệu không được trùng");
            Material material = materialRepository.findById(itemReq.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Nguyên liệu không tồn tại: ID " + itemReq.getMaterialId()));

            BigDecimal qty = itemReq.getQuantity();

            // Khóa dòng KHO và BEP
            Stock khoStock = stockRepository.findByIdWithLock(material.getId(), StockLocation.KHO)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.INVENTORY_INSUFFICIENT, "Không tìm thấy tồn KHO: " + material.getName()));

            if (khoStock.getQuantity().compareTo(qty) < 0) {
                throw BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                        "Tồn KHO không đủ để xuất: " + material.getName() + " (Còn: " + khoStock.getQuantity() + ", cần xuất: " + qty + ")");
            }

            Stock bepStock = stockRepository.findByIdWithLock(material.getId(), StockLocation.BEP)
                    .orElseGet(() -> Stock.builder()
                            .id(new StockId(material.getId(), StockLocation.BEP))
                            .material(material)
                            .quantity(BigDecimal.ZERO)
                            .threshold(BigDecimal.ZERO)
                            .build());

            // Giảm KHO
            BigDecimal khoBefore = khoStock.getQuantity();
            BigDecimal khoAfter = khoBefore.subtract(qty);
            khoStock.setQuantity(khoAfter);

            // Tăng BEP
            BigDecimal bepBefore = bepStock.getQuantity();
            BigDecimal bepAfter = bepBefore.add(qty);
            bepStock.setQuantity(bepAfter);

            stockRepository.save(khoStock);
            stockRepository.save(bepStock);

            // Lưu chi tiết xuất
            items.add(StockIssueItem.builder()
                    .id(new StockIssueItemId(issue.getId(), material.getId()))
                    .issue(issue)
                    .material(material)
                    .quantity(qty)
                    .build());

            // Ghi 2 dòng lịch sử biến động kho
            movements.add(StockMovement.builder()
                    .material(material)
                    .location(StockLocation.KHO)
                    .type(StockMovementType.XUAT_DI)
                    .deltaQuantity(qty.negate())
                    .beforeQuantity(khoBefore)
                    .afterQuantity(khoAfter)
                    .sourceType("ISSUE")
                    .sourceId(issue.getId())
                    .sourceLine(material.getId())
                    .createdBy(kitchenAccount)
                    .createdAt(Instant.now())
                    .reason(request.getReason())
                    .build());

            movements.add(StockMovement.builder()
                    .material(material)
                    .location(StockLocation.BEP)
                    .type(StockMovementType.NHAN_BEP)
                    .deltaQuantity(qty)
                    .beforeQuantity(bepBefore)
                    .afterQuantity(bepAfter)
                    .sourceType("ISSUE")
                    .sourceId(issue.getId())
                    .sourceLine(material.getId())
                    .createdBy(kitchenAccount)
                    .createdAt(Instant.now())
                    .reason(request.getReason())
                    .build());
        }

        stockIssueItemRepository.saveAll(items);
        movementRepository.saveAll(movements);

        realtimeEventPublisher.publishAfterCommit("/topic/stock", "STOCK_CHANGED", issue.getId().toString(), null, "1", java.util.Map.of("action", "ISSUE"));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STOCK_CHANGED", issue.getId().toString(), null, "1", java.util.Map.of("action", "ISSUE"));
    }

    
        @Transactional
        public void recordPreparation(
                InventoryDto.CreatePreparationBatchRequest request,
                Account kitchenAccount) {
        recordPreparation(request, kitchenAccount, null);
        }

        @Transactional
        public void recordPreparation(
                InventoryDto.CreatePreparationBatchRequest request,
                Account kitchenAccount,
                String key) {

        if (kitchenAccount == null) {
                throw BusinessException.forbidden(
                        ErrorCode.ACCESS_DENIED,
                        "Bạn chưa đăng nhập");
        }

        if (key != null && key.length() > 64) {
                throw BusinessException.badRequest(
                        ErrorCode.VALIDATION_FAILED,
                        "Mã yêu cầu quá dài");
        }

        String requestKey = (key == null || key.isBlank())
                ? UUID.randomUUID().toString()
                : key;

        accountRepository.findByIdWithLock(kitchenAccount.getId())
                .orElseThrow();

        var existing = batchRepository.findByIdempotencyKey(requestKey);

        if (existing.isPresent()) {
                PreparationBatch old = existing.get();

                boolean same =
                        old.getCreatedBy().getId().equals(kitchenAccount.getId())
                        && old.getRecipe().getId().equals(request.getRecipeId())
                        && old.getActualQuantity().compareTo(request.getActualQuantity()) == 0
                        && Objects.equals(
                                old.getDiscrepancyReason(),
                                request.getDiscrepancyReason());

                if (!same) {
                throw BusinessException.conflict(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "Mã yêu cầu đã được sử dụng cho mẻ khác");
                }

                return;
        }

        PreparationRecipe recipe = recipeRepository
                .findById(request.getRecipeId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.VALIDATION_FAILED,
                        "Công thức sơ chế không tồn tại: ID " + request.getRecipeId()
                ));

        List<PreparationRecipeItem> recipeItems = recipeItemRepository.findByIdRecipeId(recipe.getId());
        if (recipeItems.isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Công thức sơ chế chưa cấu hình nguyên liệu đầu vào");
        }

        PreparationBatch batch = PreparationBatch.builder()
                .recipe(recipe)
                .outputMaterial(recipe.getOutputMaterial())
                .expectedQuantity(recipe.getStandardOutputQuantity())
                .actualQuantity(request.getActualQuantity())
                .status(DocumentStatus.DA_GHI_SO)
                .discrepancyReason(request.getDiscrepancyReason())
                .createdBy(kitchenAccount)
                .postedAt(Instant.now())
                .idempotencyKey(requestKey)
                .build();
        batch = batchRepository.save(batch);

        List<PreparationBatchItem> batchItems = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        // Giảm nguyên liệu đầu vào (THO) tại BEP
        for (PreparationRecipeItem item : recipeItems) {
            Material inMat = item.getInputMaterial();
            BigDecimal inQty = item.getStandardInputQuantity();

            Stock inStock = stockRepository.findByIdWithLock(inMat.getId(), StockLocation.BEP)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.INVENTORY_INSUFFICIENT,
                            "Không tìm thấy tồn tại Bếp cho nguyên liệu đầu vào: " + inMat.getName()));

            if (inStock.getQuantity().compareTo(inQty) < 0) {
                throw BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                        "Nguyên liệu đầu vào tại Bếp không đủ để nấu mẻ: " + inMat.getName() + " (Còn: " + inStock.getQuantity() + ", cần: " + inQty + ")");
            }

            BigDecimal before = inStock.getQuantity();
            BigDecimal after = before.subtract(inQty);
            inStock.setQuantity(after);
            stockRepository.save(inStock);

            batchItems.add(PreparationBatchItem.builder()
                    .id(new PreparationBatchItemId(batch.getId(), inMat.getId()))
                    .batch(batch)
                    .material(inMat)
                    .recipeQuantity(inQty)
                    .actualQuantity(inQty)
                    .build());

            movements.add(StockMovement.builder()
                    .material(inMat)
                    .location(StockLocation.BEP)
                    .type(StockMovementType.SOCHE_RA)
                    .deltaQuantity(inQty.negate())
                    .beforeQuantity(before)
                    .afterQuantity(after)
                    .sourceType("BATCH")
                    .sourceId(batch.getId())
                    .sourceLine(inMat.getId())
                    .createdBy(kitchenAccount)
                    .createdAt(Instant.now())
                    .reason("Nguyên liệu đầu vào cho mẻ sơ chế #" + batch.getId())
                    .build());
        }

        // Tăng nguyên liệu đầu ra (SOCHE) tại BEP
        Material outMat = recipe.getOutputMaterial();
        Stock outStock = stockRepository.findByIdWithLock(outMat.getId(), StockLocation.BEP)
                .orElseGet(() -> Stock.builder()
                        .id(new StockId(outMat.getId(), StockLocation.BEP))
                        .material(outMat)
                        .quantity(BigDecimal.ZERO)
                        .threshold(BigDecimal.ZERO)
                        .build());

        BigDecimal outBefore = outStock.getQuantity();
        BigDecimal outAfter = outBefore.add(request.getActualQuantity());
        outStock.setQuantity(outAfter);
        stockRepository.save(outStock);

        movements.add(StockMovement.builder()
                .material(outMat)
                .location(StockLocation.BEP)
                .type(StockMovementType.SOCHE_VAO)
                .deltaQuantity(request.getActualQuantity())
                .beforeQuantity(outBefore)
                .afterQuantity(outAfter)
                .sourceType("BATCH")
                .sourceId(batch.getId())
                .sourceLine(outMat.getId())
                .createdBy(kitchenAccount)
                .createdAt(Instant.now())
                .reason("Sản lượng thực thu mẻ sơ chế #" + batch.getId())
                .build());

        batchItemRepository.saveAll(batchItems);
        movementRepository.saveAll(movements);

        realtimeEventPublisher.publishAfterCommit("/topic/stock", "STOCK_CHANGED", batch.getId().toString(), null, "1", java.util.Map.of("action", "PREPARATION"));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STOCK_CHANGED", batch.getId().toString(), null, "1", java.util.Map.of("action", "PREPARATION"));
    }

    @Transactional
    public void configureThreshold(InventoryDto.UpdateThresholdRequest request) {
        Stock stock = stockRepository.findByIdWithLock(request.getMaterialId(), request.getLocation())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu tại vị trí " + request.getLocation()));

        stock.setThreshold(request.getThreshold());
        stockRepository.save(stock);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.StockItemResponse> getStocks(StockLocation location) {
        List<Stock> stocks = stockRepository.findByIdLocation(location);
        return stocks.stream().map(s -> InventoryDto.StockItemResponse.builder()
                .materialId(s.getId().getMaterialId())
                .materialName(s.getMaterial().getName())
                .type(s.getMaterial().getType())
                .unit(s.getMaterial().getUnit())
                .location(s.getId().getLocation())
                .quantity(s.getQuantity())
                .threshold(s.getThreshold())
                .isLowStock(s.getQuantity().compareTo(s.getThreshold()) <= 0)
                .build()
        ).toList();
    }

    // ==========================================
    // NHẬP KHO (KHO)
    // ==========================================
    @Transactional
    public InventoryDto.ImportReceiptResponse recordImport(InventoryDto.CreateImportReceiptRequest request, Account account) {
        ImportReceipt receipt = ImportReceipt.builder()
                .createdBy(account)
                .reason(request.getReason())
                .supplier(request.getSupplier())
                .status(DocumentStatus.DA_GHI_SO)
                .createdAt(Instant.now())
                .postedAt(Instant.now())
                .build();
        receipt = importReceiptRepository.save(receipt);

        List<ImportReceiptItem> items = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        for (var it : request.getItems()) {
            Material material = materialRepository.findById(it.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Nguyên liệu không tồn tại: ID " + it.getMaterialId()));

            Stock khoStock = stockRepository.findByIdWithLock(material.getId(), StockLocation.KHO)
                    .orElseGet(() -> Stock.builder()
                            .id(new StockId(material.getId(), StockLocation.KHO))
                            .material(material)
                            .quantity(BigDecimal.ZERO)
                            .threshold(BigDecimal.ZERO)
                            .build());

            BigDecimal before = khoStock.getQuantity();
            BigDecimal after = before.add(it.getQuantity());
            khoStock.setQuantity(after);
            stockRepository.save(khoStock);

            items.add(ImportReceiptItem.builder()
                    .id(new ImportReceiptItemId(receipt.getId(), material.getId()))
                    .receipt(receipt)
                    .material(material)
                    .quantity(it.getQuantity())
                    .unitPrice(it.getUnitPrice())
                    .build());

            movements.add(StockMovement.builder()
                    .material(material)
                    .location(StockLocation.KHO)
                    .type(StockMovementType.NHAP)
                    .deltaQuantity(it.getQuantity())
                    .beforeQuantity(before)
                    .afterQuantity(after)
                    .sourceType("IMPORT")
                    .sourceId(receipt.getId())
                    .sourceLine(material.getId())
                    .createdBy(account)
                    .createdAt(Instant.now())
                    .reason(request.getReason())
                    .build());
        }

        importReceiptItemRepository.saveAll(items);
        movementRepository.saveAll(movements);

        realtimeEventPublisher.publishAfterCommit("/topic/stock", "STOCK_CHANGED", receipt.getId().toString(), null, "1", java.util.Map.of("action", "IMPORT"));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STOCK_CHANGED", receipt.getId().toString(), null, "1", java.util.Map.of("action", "IMPORT"));

        return InventoryDto.ImportReceiptResponse.builder()
                .id(receipt.getId())
                .reason(receipt.getReason())
                .supplier(receipt.getSupplier())
                .status(receipt.getStatus().name())
                .createdAt(receipt.getCreatedAt())
                .createdByEmail(account != null ? account.getEmail() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.ImportReceiptResponse> listImports() {
        return importReceiptRepository.findAll().stream().map(r -> InventoryDto.ImportReceiptResponse.builder()
                .id(r.getId())
                .reason(r.getReason())
                .supplier(r.getSupplier())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .createdByEmail(r.getCreatedBy() != null ? r.getCreatedBy().getEmail() : null)
                .build()
        ).toList();
    }

    // ==========================================
    // HAO HỤT / HỦY KHO (WASTE)
    // ==========================================
    @Transactional
    public void recordWaste(InventoryDto.CreateWasteRequest request, Account account) {
        List<StockMovement> movements = new ArrayList<>();

        for (var it : request.getItems()) {
            Material material = materialRepository.findById(it.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Nguyên liệu không tồn tại: ID " + it.getMaterialId()));

            Stock stock = stockRepository.findByIdWithLock(material.getId(), request.getLocation())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.INVENTORY_INSUFFICIENT,
                            "Không tìm thấy tồn kho cho nguyên liệu: " + material.getName()));

            if (stock.getQuantity().compareTo(it.getQuantity()) < 0) {
                throw BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                        "Tồn kho không đủ để hủy: " + material.getName() + " (Còn: " + stock.getQuantity() + ", cần hủy: " + it.getQuantity() + ")");
            }

            BigDecimal before = stock.getQuantity();
            BigDecimal after = before.subtract(it.getQuantity());
            stock.setQuantity(after);
            stockRepository.save(stock);

            movements.add(StockMovement.builder()
                    .material(material)
                    .location(request.getLocation())
                    .type(StockMovementType.HAO_HUT)
                    .deltaQuantity(it.getQuantity().negate())
                    .beforeQuantity(before)
                    .afterQuantity(after)
                    .sourceType("WASTE")
                    .sourceId(material.getId())
                    .createdBy(account)
                    .createdAt(Instant.now())
                    .reason(request.getReason())
                    .build());
        }

        movementRepository.saveAll(movements);

        auditRepository.save(vn.edu.ute.milktea.entity.audit.BusinessAudit.builder()
                .accountId(account != null ? account.getId() : null)
                .action("INVENTORY_WASTE_RECORDED")
                .reason(request.getReason())
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/stock", "STOCK_CHANGED", request.getLocation().name(), null, "1", java.util.Map.of("action", "WASTE"));
        realtimeEventPublisher.publishAfterCommit("/topic/admin", "STOCK_CHANGED", request.getLocation().name(), null, "1", java.util.Map.of("action", "WASTE"));
    }

    // ==========================================
    // LỊCH SỬ BIẾN ĐỘNG KHO (MOVEMENTS)
    // ==========================================
    @Transactional(readOnly = true)
    public List<InventoryDto.StockMovementResponse> listMovements(Long materialId, StockLocation location) {
        List<StockMovement> list;
        if (materialId != null && location != null) {
            list = movementRepository.findByMaterialIdAndLocationOrderByCreatedAtDesc(materialId, location);
        } else {
            list = movementRepository.findTop100ByOrderByCreatedAtDesc();
        }
        return list.stream().map(m -> InventoryDto.StockMovementResponse.builder()
                .id(m.getId())
                .materialId(m.getMaterial().getId())
                .materialName(m.getMaterial().getName())
                .location(m.getLocation())
                .type(m.getType().name())
                .deltaQuantity(m.getDeltaQuantity())
                .beforeQuantity(m.getBeforeQuantity())
                .afterQuantity(m.getAfterQuantity())
                .sourceType(m.getSourceType())
                .sourceId(m.getSourceId())
                .reason(m.getReason())
                .createdAt(m.getCreatedAt())
                .build()
        ).toList();
    }

    // ==========================================
    // SỰ CỐ PHA CHẾ & PHA BÙ (INCIDENTS & REMAKE)
    // ==========================================
    @Transactional
    public InventoryDto.IncidentResponse recordIncident(Long orderId, InventoryDto.IncidentRequest request, Account kitchenAccount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng #" + orderId));

        vn.edu.ute.milktea.entity.audit.BusinessAudit audit = vn.edu.ute.milktea.entity.audit.BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(kitchenAccount != null ? kitchenAccount.getId() : null)
                .action("ORDER_INCIDENT_RECORDED")
                .reason(request.getReason() + (request.getNotes() != null ? " (" + request.getNotes() + ")" : ""))
                .createdAt(Instant.now())
                .build();
        audit = auditRepository.save(audit);

        return InventoryDto.IncidentResponse.builder()
                .id(audit.getId())
                .orderId(order.getId())
                .reason(audit.getReason())
                .createdAt(audit.getCreatedAt())
                .createdByEmail(kitchenAccount != null ? kitchenAccount.getEmail() : null)
                .resolved(false)
                .build();
    }

    @Transactional
    public void resolveIncident(Long orderId, Long incidentId, Account account) {
        auditRepository.save(vn.edu.ute.milktea.entity.audit.BusinessAudit.builder()
                .orderId(orderId)
                .accountId(account != null ? account.getId() : null)
                .action("ORDER_INCIDENT_RESOLVED")
                .reason("Giải quyết sự cố #" + incidentId)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.IncidentResponse> getIncidents(Long orderId) {
        List<vn.edu.ute.milktea.entity.audit.BusinessAudit> audits = auditRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return audits.stream()
                .filter(a -> "ORDER_INCIDENT_RECORDED".equals(a.getAction()))
                .map(a -> InventoryDto.IncidentResponse.builder()
                        .id(a.getId())
                        .orderId(a.getOrderId())
                        .reason(a.getReason())
                        .createdAt(a.getCreatedAt())
                        .createdByEmail(null)
                        .resolved(false)
                        .build()
                ).toList();
    }

    @Transactional
    public void recordAdditionalConsumption(Long orderId, InventoryDto.AdditionalConsumptionRequest request, Account kitchenAccount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng #" + orderId));

        List<StockMovement> movements = new ArrayList<>();

        for (var item : request.getItems()) {
            Material material = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Nguyên liệu không tồn tại: ID " + item.getMaterialId()));

            Stock stock = stockRepository.findByIdWithLock(material.getId(), StockLocation.BEP)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.INVENTORY_INSUFFICIENT,
                            "Không tìm thấy tồn tại Bếp cho nguyên liệu: " + material.getName()));

            if (stock.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                        "Tồn tại Bếp không đủ để pha bù: " + material.getName() + " (Còn: " + stock.getQuantity() + ", cần: " + item.getQuantity() + ")");
            }

            BigDecimal before = stock.getQuantity();
            BigDecimal after = before.subtract(item.getQuantity());
            stock.setQuantity(after);
            stockRepository.save(stock);

            movements.add(StockMovement.builder()
                    .material(material)
                    .location(StockLocation.BEP)
                    .type(StockMovementType.TIEU_HAO)
                    .deltaQuantity(item.getQuantity().negate())
                    .beforeQuantity(before)
                    .afterQuantity(after)
                    .sourceType("REMAKE")
                    .sourceId(order.getId())
                    .sourceLine(material.getId())
                    .createdBy(kitchenAccount)
                    .createdAt(Instant.now())
                    .reason("Pha bù đơn hàng #" + order.getId() + ": " + request.getReason())
                    .build());
        }

        movementRepository.saveAll(movements);

        auditRepository.save(vn.edu.ute.milktea.entity.audit.BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(kitchenAccount != null ? kitchenAccount.getId() : null)
                .action("ORDER_REMAKE_CONSUMED")
                .reason(request.getReason())
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/stock", "STOCK_CHANGED", order.getId().toString(), null, "1", java.util.Map.of("action", "REMAKE"));
    }
}
