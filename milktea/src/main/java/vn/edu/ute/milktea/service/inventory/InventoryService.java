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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final MaterialRepository materialRepository;
    private final vn.edu.ute.milktea.repository.account.AccountRepository accountRepository;
    private final PreparationBatchRepository batchRepository;
    private final PreparationBatchItemRepository batchItemRepository;
    private final PreparationRecipeRepository recipeRepository;
    private final PreparationRecipeItemRepository recipeItemRepository;
    private final OrderIngredientSnapshotRepository snapshotRepository;
    private final vn.edu.ute.milktea.repository.order.OrderItemRepository orderItemRepository;
    private final vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository productRecipeRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ImportReceiptItemRepository importReceiptItemRepository;
    private final ShiftSettlementRepository shiftSettlementRepository;
    private final ShiftSettlementItemRepository shiftSettlementItemRepository;
    private final vn.edu.ute.milktea.repository.audit.BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.repository.order.OrderRepository orderRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    @Transactional(readOnly = true)
    public BigDecimal getMaterialCostPrice(Long materialId) {
        List<ImportReceiptItem> importItems = importReceiptItemRepository.findByIdMaterialId(materialId);
        if (importItems.isEmpty()) {
            return null;
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (ImportReceiptItem item : importItems) {
            if (item.getQuantity() != null && item.getQuantity().signum() > 0 && item.getUnitPrice() != null) {
                totalCost = totalCost.add(item.getQuantity().multiply(item.getUnitPrice()));
                totalQty = totalQty.add(item.getQuantity());
            }
        }
        if (totalQty.signum() <= 0) {
            return null;
        }
        return totalCost.divide(totalQty, 6, RoundingMode.HALF_UP);
    }

    @Transactional
    public void completeOrderConsumption(Order order, Account kitchenAccount) {
        // Snapshot được chốt khi tạo đơn; không tính lại từ định mức CongThucMon.
        List<OrderIngredientSnapshot> snapshots = snapshotRepository.findByIdOrderId(order.getId());
        if (snapshots.isEmpty()) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                    "Đơn hàng chưa có dữ liệu nguyên liệu tiêu hao: #" + order.getId());
        }
        // Khóa tồn kho theo thứ tự cố định (mã nguyên liệu, vị trí).
        List<OrderIngredientSnapshot> sortedSnapshots = snapshots.stream()
                .sorted(Comparator.comparing((OrderIngredientSnapshot s) -> s.getMaterial().getId())
                        .thenComparing(s -> s.getMaterial().getType().name()))
                .toList();
        List<StockMovement> movements = new ArrayList<>();
        for (OrderIngredientSnapshot snap : sortedSnapshots) {
            Material material = snap.getMaterial();
            BigDecimal needed = snap.getTotalQuantity();
            if (needed == null || needed.signum() <= 0) {
                throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                        "Số lượng tiêu hao không hợp lệ: " + material.getName());
            }
            StockLocation location = snap.getSnapshotType() == MaterialType.SOCHE ? StockLocation.BEP : StockLocation.KHO;
            var stockOpt = stockRepository.findByIdWithLock(material.getId(), location);
            if (stockOpt.isEmpty()) {
                StockLocation alternate = location == StockLocation.BEP ? StockLocation.KHO : StockLocation.BEP;
                var altOpt = stockRepository.findByIdWithLock(material.getId(), alternate);
                if (altOpt.isPresent()) {
                    stockOpt = altOpt;
                    location = alternate;
                }
            }
            final StockLocation actualLocation = location;
            Stock stock = stockOpt.orElseThrow(() -> BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                    "Không tìm thấy tồn kho " + material.getName() + " tại " + actualLocation));
            BigDecimal before = stock.getQuantity();
            if (before.compareTo(needed) < 0) {
                String unit = material.getUnit() != null ? material.getUnit() : "";
                throw BusinessException.conflict(ErrorCode.INVENTORY_INSUFFICIENT,
                        "Không đủ tồn kho " + material.getName() + " tại " + actualLocation
                                + ". Cần: " + needed + (unit.isEmpty() ? "" : " " + unit)
                                + ", còn: " + before + (unit.isEmpty() ? "" : " " + unit));
            }
            BigDecimal after = before.subtract(needed);
            stock.setQuantity(after);
            stockRepository.save(stock);
            movements.add(StockMovement.builder()
                    .material(material).location(actualLocation).type(StockMovementType.TIEU_HAO)
                    .deltaQuantity(needed.negate()).beforeQuantity(before).afterQuantity(after)
                    .sourceType("ORDER").sourceId(order.getId()).sourceLine(material.getId())
                    .createdBy(kitchenAccount).createdAt(Instant.now())
                    .reason("Tiêu hao đơn hàng #" + order.getId()).build());
        }
        movementRepository.saveAll(movements);
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
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn chưa đăng nhập");
        }
        if (key != null && key.length() > 64) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    "Mã yêu cầu quá dài");
        }

        BigDecimal desiredQuantity = request.getDesiredQuantity() != null
                ? request.getDesiredQuantity()
                : request.getActualQuantity();

        if (desiredQuantity == null || desiredQuantity.signum() <= 0) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    "Sản lượng thành phẩm mong muốn phải lớn hơn 0");
        }

        String requestKey = (key == null || key.isBlank())
                ? UUID.randomUUID().toString()
                : key;

        accountRepository.findByIdWithLock(kitchenAccount.getId())
                .orElseThrow();

        PreparationRecipe recipe;
        if (request.getRecipeId() != null) {
            recipe = recipeRepository.findById(request.getRecipeId())
                    .orElseThrow(() -> BusinessException.notFound(
                            ErrorCode.VALIDATION_FAILED,
                            "Công thức sơ chế không tồn tại: ID " + request.getRecipeId()));
        } else if (request.getOutputMaterialId() != null) {
            recipe = recipeRepository.findByOutputMaterialIdAndActiveTrue(request.getOutputMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(
                            ErrorCode.VALIDATION_FAILED,
                            "Không tìm thấy định mức sơ chế đang áp dụng cho thành phẩm ID " + request.getOutputMaterialId()));
        } else {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    "Vui lòng chọn thành phẩm sơ chế");
        }

        if (!Boolean.TRUE.equals(recipe.getActive())) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    "Định mức sơ chế cho thành phẩm này hiện đang tạm dừng hoặc không còn hiệu lực.");
        }

        if (request.getRecipeVersion() != null && recipe.getVersion() != null
                && !recipe.getVersion().equals(request.getRecipeVersion())) {
            throw BusinessException.conflict(
                    ErrorCode.ORDER_STATE_CONFLICT,
                    "Định mức sơ chế đã được Quản trị viên cập nhật phiên bản mới. Vui lòng tải lại thông tin thành phẩm!");
        }

        var existing = batchRepository.findByIdempotencyKey(requestKey);
        if (existing.isPresent()) {
            PreparationBatch old = existing.get();
            boolean same = old.getCreatedBy().getId().equals(kitchenAccount.getId())
                    && old.getRecipe().getId().equals(recipe.getId())
                    && old.getActualQuantity().compareTo(desiredQuantity) == 0;

            if (!same) {
                throw BusinessException.conflict(
                        ErrorCode.IDEMPOTENCY_CONFLICT,
                        "Mã yêu cầu đã được sử dụng cho mẻ khác");
            }
            return;
        }

        List<PreparationRecipeItem> recipeItems = recipeItemRepository.findByIdRecipeId(recipe.getId());
        if (recipeItems.isEmpty()) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    "Định mức sơ chế chưa cấu hình nguyên liệu đầu vào");
        }

        BigDecimal stdOutput = recipe.getStandardOutputQuantity();
        if (stdOutput == null || stdOutput.signum() <= 0) {
            throw BusinessException.badRequest(
                    ErrorCode.VALIDATION_FAILED,
                    "Sản lượng chuẩn của định mức không hợp lệ");
        }

        // Hệ số mẻ = lượng thành phẩm mong muốn / sản lượng đầu ra của định mức
        BigDecimal batchScale = desiredQuantity.divide(stdOutput, 12, RoundingMode.HALF_UP);

        class BatchInputRecord {
            Material material;
            BigDecimal recipeQuantity;
            BigDecimal actualQuantity;
            BigDecimal unitPrice;
            BigDecimal totalCost;
        }

        List<BatchInputRecord> inputRecords = new ArrayList<>();
        BigDecimal totalBatchCost = BigDecimal.ZERO;

        for (PreparationRecipeItem rItem : recipeItems) {
            Material inMat = rItem.getInputMaterial();
            BigDecimal stdInput = rItem.getStandardInputQuantity();
            if (stdInput == null || stdInput.signum() <= 0) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Định lượng đầu vào không hợp lệ cho nguyên liệu: " + inMat.getName());
            }

            // Lượng nguyên liệu thô cần dùng = lượng đầu vào định mức × hệ số mẻ
            BigDecimal neededInputQty = stdInput.multiply(batchScale);

            BigDecimal unitPrice = getMaterialCostPrice(inMat.getId());
            if (unitPrice == null) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Chưa đủ dữ liệu giá vốn cho nguyên liệu: " + inMat.getName());
            }

            BigDecimal itemCost = neededInputQty.multiply(unitPrice);
            totalBatchCost = totalBatchCost.add(itemCost);

            BatchInputRecord bir = new BatchInputRecord();
            bir.material = inMat;
            bir.recipeQuantity = neededInputQty;
            bir.actualQuantity = neededInputQty;
            bir.unitPrice = unitPrice;
            bir.totalCost = itemCost;
            inputRecords.add(bir);
        }

        // Giá vốn 1 đơn vị thành phẩm = chi phí định mức / sản lượng đầu ra định mức (tương đương totalBatchCost / desiredQuantity)
        BigDecimal unitCost = totalBatchCost.divide(desiredQuantity, 12, RoundingMode.HALF_UP);

        String note = request.getNote() != null ? request.getNote().trim() : request.getDiscrepancyReason();

        PreparationBatch batch = PreparationBatch.builder()
                .recipe(recipe)
                .outputMaterial(recipe.getOutputMaterial())
                .expectedQuantity(stdOutput)
                .actualQuantity(desiredQuantity)
                .status(DocumentStatus.DA_GHI_SO)
                .discrepancyReason(note)
                .createdBy(kitchenAccount)
                .postedAt(Instant.now())
                .idempotencyKey(requestKey)
                .totalCost(totalBatchCost.setScale(2, RoundingMode.HALF_UP))
                .unitCost(unitCost)
                .ratioVariancePercent(BigDecimal.ZERO)
                .toleranceWarning(false)
                .build();
        batch = batchRepository.save(batch);

        List<PreparationBatchItem> batchItems = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        for (BatchInputRecord bir : inputRecords) {
            Material inMat = bir.material;
            BigDecimal inQty = bir.actualQuantity;

            StockLocation inLoc = StockLocation.BEP;
            var inStockOpt = stockRepository.findByIdWithLock(inMat.getId(), StockLocation.BEP);
            if (inStockOpt.isEmpty() || inStockOpt.get().getQuantity().compareTo(inQty) < 0) {
                var khoStockOpt = stockRepository.findByIdWithLock(inMat.getId(), StockLocation.KHO);
                if (khoStockOpt.isPresent() && (inStockOpt.isEmpty() || khoStockOpt.get().getQuantity().compareTo(inQty) >= 0)) {
                    inLoc = StockLocation.KHO;
                    inStockOpt = khoStockOpt;
                }
            }

            Stock inStock = inStockOpt.orElseThrow(() -> BusinessException.notFound(
                    ErrorCode.INVENTORY_INSUFFICIENT,
                    "Không tìm thấy nguyên liệu đầu vào tại kho: " + inMat.getName()));

            if (inStock.getQuantity().compareTo(inQty) < 0) {
                String unit = inMat.getUnit() != null ? inMat.getUnit() : "";
                throw BusinessException.conflict(
                        ErrorCode.INVENTORY_INSUFFICIENT,
                        "Nguyên liệu đầu vào không đủ: " + inMat.getName()
                                + " (Còn: " + inStock.getQuantity() + " " + unit
                                + ", cần: " + inQty + " " + unit + ")");
            }

            BigDecimal before = inStock.getQuantity();
            BigDecimal after = before.subtract(inQty);
            inStock.setQuantity(after);
            stockRepository.save(inStock);

            batchItems.add(PreparationBatchItem.builder()
                    .id(new PreparationBatchItemId(batch.getId(), inMat.getId()))
                    .batch(batch)
                    .material(inMat)
                    .recipeQuantity(bir.recipeQuantity)
                    .actualQuantity(inQty)
                    .unitPrice(bir.unitPrice)
                    .totalCost(bir.totalCost)
                    .build());

            movements.add(StockMovement.builder()
                    .material(inMat)
                    .location(inLoc)
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

        Material outMat = recipe.getOutputMaterial();
        Stock outStock = stockRepository.findByIdWithLock(outMat.getId(), StockLocation.BEP)
                .orElseGet(() -> Stock.builder()
                        .id(new StockId(outMat.getId(), StockLocation.BEP))
                        .material(outMat)
                        .quantity(BigDecimal.ZERO)
                        .threshold(BigDecimal.ZERO)
                        .build());

        BigDecimal outBefore = outStock.getQuantity();
        BigDecimal outAfter = outBefore.add(desiredQuantity);
        outStock.setQuantity(outAfter);
        stockRepository.save(outStock);

        movements.add(StockMovement.builder()
                .material(outMat)
                .location(StockLocation.BEP)
                .type(StockMovementType.SOCHE_VAO)
                .deltaQuantity(desiredQuantity)
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

        realtimeEventPublisher.publishAfterCommit(
                "/topic/stock",
                "STOCK_CHANGED",
                batch.getId().toString(),
                null,
                "1",
                java.util.Map.of("action", "PREPARATION"));
        realtimeEventPublisher.publishAfterCommit(
                "/topic/admin",
                "STOCK_CHANGED",
                batch.getId().toString(),
                null,
                "1",
                java.util.Map.of("action", "PREPARATION"));
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.PreparationBatchResponse> listPreparationBatches() {
        return listPreparationBatches(null, null, true);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.PreparationBatchResponse> listPreparationBatches(LocalDate from, LocalDate to, boolean all) {
        List<PreparationBatch> batches;
        if (all) {
            batches = batchRepository.findAllByOrderByPostedAtDescIdDesc();
        } else {
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDate f = (from != null) ? from : LocalDate.now(zone);
            LocalDate t = (to != null) ? to : LocalDate.now(zone);
            Instant fromInstant = f.atStartOfDay(zone).toInstant();
            Instant toInstant = t.atTime(LocalTime.MAX).atZone(zone).toInstant();
            batches = batchRepository.findByPostedAtBetweenOrderByPostedAtDescIdDesc(fromInstant, toInstant);
        }
        return batches.stream().map(this::mapPreparationBatch).toList();
    }

    private InventoryDto.PreparationBatchResponse mapPreparationBatch(PreparationBatch batch) {
        BigDecimal expected = batch.getExpectedQuantity() != null
                ? batch.getExpectedQuantity()
                : BigDecimal.ZERO;
        BigDecimal actual = batch.getActualQuantity() != null
                ? batch.getActualQuantity()
                : BigDecimal.ZERO;
        Account creator = batch.getCreatedBy();
        Material output = batch.getOutputMaterial();
        PreparationRecipe recipe = batch.getRecipe();

        List<InventoryDto.PreparationBatchItemResponse> items = batchItemRepository.findByIdBatchId(batch.getId())
                .stream()
                .map(bi -> InventoryDto.PreparationBatchItemResponse.builder()
                        .materialId(bi.getMaterial().getId())
                        .materialName(bi.getMaterial().getName())
                        .unit(bi.getMaterial().getUnit())
                        .recipeQuantity(bi.getRecipeQuantity())
                        .actualQuantity(bi.getActualQuantity())
                        .unitPrice(bi.getUnitPrice())
                        .totalCost(bi.getTotalCost())
                        .build())
                .toList();

        return InventoryDto.PreparationBatchResponse.builder()
                .id(batch.getId())
                .postedAt(batch.getPostedAt())
                .recipeId(recipe != null ? recipe.getId() : null)
                .recipeName(recipe != null ? recipe.getName() : null)
                .recipeVersion(recipe != null ? (recipe.getVersion() != null ? recipe.getVersion() : 1L) : 1L)
                .outputMaterialId(output != null ? output.getId() : null)
                .outputMaterialName(output != null ? output.getName() : null)
                .expectedQuantity(expected)
                .actualQuantity(actual)
                .differenceQuantity(actual.subtract(expected))
                .unit(output != null ? output.getUnit() : null)
                .discrepancyReason(batch.getDiscrepancyReason())
                .note(batch.getDiscrepancyReason())
                .createdByEmail(creator != null ? creator.getEmail() : null)
                .createdByName(creator != null ? creator.getFullName() : null)
                .status(batch.getStatus() != null ? batch.getStatus().name() : null)
                .totalCost(batch.getTotalCost())
                .unitCost(batch.getUnitCost())
                .ratioVariancePercent(batch.getRatioVariancePercent())
                .toleranceWarning(batch.getToleranceWarning())
                .items(items)
                .build();
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
        return stocks.stream().map(this::mapStockToResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDto.StockSummaryResponse getStocksWithFilter(StockLocation location, String alertFilter, String query) {
        List<Stock> stocks;
        if (location != null) {
            stocks = stockRepository.findByIdLocation(location);
        } else {
            stocks = stockRepository.findAll();
        }

        List<InventoryDto.StockItemResponse> allItems = stocks.stream()
                .filter(s -> s.getMaterial() != null && Boolean.TRUE.equals(s.getMaterial().getActive()))
                .map(this::mapStockToResponse)
                .sorted(Comparator.comparing(InventoryDto.StockItemResponse::getMaterialName))
                .toList();

        long totalCount = allItems.size();
        long hetTonCount = allItems.stream().filter(it -> "HET_TON".equals(it.getAlertStatus())).count();
        long tonThapCount = allItems.stream().filter(it -> "TON_THAP".equals(it.getAlertStatus())).count();
        long binhThuongCount = allItems.stream().filter(it -> "BINH_THUONG".equals(it.getAlertStatus())).count();
        long chuaCauHinhCount = allItems.stream().filter(it -> "CHUA_CAU_HINH".equals(it.getAlertStatus())).count();

        var stream = allItems.stream();
        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            stream = stream.filter(it -> it.getMaterialName().toLowerCase().contains(q));
        }

        if (alertFilter != null && !alertFilter.isBlank() && !"ALL".equalsIgnoreCase(alertFilter)) {
            String filter = alertFilter.trim().toUpperCase();
            if ("HET_TON".equals(filter)) {
                stream = stream.filter(it -> "HET_TON".equals(it.getAlertStatus()));
            } else if ("TON_THAP".equals(filter)) {
                stream = stream.filter(it -> "TON_THAP".equals(it.getAlertStatus()));
            } else if ("BINH_THUONG".equals(filter)) {
                stream = stream.filter(it -> "BINH_THUONG".equals(it.getAlertStatus()));
            } else if ("CHUA_CAU_HINH".equals(filter)) {
                stream = stream.filter(it -> "CHUA_CAU_HINH".equals(it.getAlertStatus()));
            }
        }

        return InventoryDto.StockSummaryResponse.builder()
                .totalCount(totalCount)
                .hetTonCount(hetTonCount)
                .tonThapCount(tonThapCount)
                .binhThuongCount(binhThuongCount)
                .chuaCauHinhCount(chuaCauHinhCount)
                .items(stream.toList())
                .build();
    }

    private InventoryDto.StockItemResponse mapStockToResponse(Stock s) {
        BigDecimal avgPrice = null;
        if (s.getMaterial().getType() == MaterialType.THO) {
            avgPrice = getMaterialCostPrice(s.getId().getMaterialId());
        } else if (s.getMaterial().getType() == MaterialType.SOCHE) {
            try {
                avgPrice = calculateWeightedAverageUnitCost(s.getId().getMaterialId(), null, null, null, null);
            } catch (Exception ignored) {
                avgPrice = null;
            }
        }
        BigDecimal qty = s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO;
        BigDecimal th = s.getThreshold();

        String alertStatus;
        String alertLabel;
        boolean isLow = false;

        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            alertStatus = "HET_TON";
            alertLabel = "Hết tồn";
            isLow = true;
        } else if (th == null) {
            alertStatus = "CHUA_CAU_HINH";
            alertLabel = "Chưa cấu hình";
        } else if (qty.compareTo(th) < 0) {
            alertStatus = "TON_THAP";
            alertLabel = "Tồn thấp";
            isLow = true;
        } else {
            alertStatus = "BINH_THUONG";
            alertLabel = "Bình thường";
        }

        return InventoryDto.StockItemResponse.builder()
                .materialId(s.getId().getMaterialId())
                .materialName(s.getMaterial().getName())
                .type(s.getMaterial().getType())
                .unit(s.getMaterial().getUnit())
                .location(s.getId().getLocation())
                .quantity(qty)
                .threshold(th)
                .isLowStock(isLow)
                .alertStatus(alertStatus)
                .alertLabel(alertLabel)
                .averageImportPrice(avgPrice)
                .build();
    }

    // ==========================================
    // NHẬP KHO (KHO)

    // ==========================================

    @Transactional
    public InventoryDto.ImportReceiptResponse recordImport(
                InventoryDto.CreateImportReceiptRequest request,
                Account account) {
        ImportReceipt receipt = ImportReceipt.builder()
                .createdBy(account)
                .reason(request.getReason())
                .supplier(request.getSupplier())
                .status(DocumentStatus.DA_GHI_SO)
                .createdAt(Instant.now())
                .postedAt(Instant.now())
                // Tạo mã yêu cầu duy nhất cho phiếu nhập
                .idempotencyKey(java.util.UUID.randomUUID().toString())
                .build();
        receipt = importReceiptRepository.save(receipt);
        List<ImportReceiptItem> items = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();
        for (var it : request.getItems()) {
            Material material = materialRepository.findById(it.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Nguyên liệu không tồn tại: ID " + it.getMaterialId()));
                    if (material.getType() != MaterialType.THO) {
                        throw BusinessException.badRequest(
                                ErrorCode.VALIDATION_FAILED,
                                "Kho tổng chỉ được nhập nguyên liệu thô: " + material.getName()
                        );
                        }
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
            
            BigDecimal unitCost;

            if (material.getType() == MaterialType.SOCHE) {
                unitCost = calculateWeightedAverageUnitCost(
                        material.getId(),
                        null,
                        null,
                        null,
                        null
                );
            } else {
                unitCost = getMaterialCostPrice(material.getId());
            }

            if (unitCost == null || unitCost.signum() <= 0) {
                throw BusinessException.badRequest(
                        ErrorCode.VALIDATION_FAILED,
                        "Chưa có giá vốn hợp lệ cho nguyên liệu: "
                                + material.getName()
                );
            }

            BigDecimal lossCost = it.getQuantity()
                    .multiply(unitCost)
                    .setScale(2, RoundingMode.HALF_UP);

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
                    .unitCost(unitCost)
                    .lossCost(lossCost)
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

    // =========================================================================
    // TÍNH GIÁ VỐN BÌNH QUÂN VÀ KẾT CA KHO / HAO HỤT
    // =========================================================================

    @Transactional(readOnly = true)
    public BigDecimal calculateEstimatedRecipeUnitCost(Long outputMaterialId) {
        PreparationRecipe recipe = recipeRepository.findByOutputMaterialIdAndActiveTrue(outputMaterialId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy định mức sơ chế đang áp dụng cho thành phẩm ID: " + outputMaterialId));

        if (recipe.getStandardOutputQuantity() == null || recipe.getStandardOutputQuantity().signum() <= 0) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Sản lượng đầu ra của định mức sơ chế không hợp lệ");
        }

        List<PreparationRecipeItem> items = recipeItemRepository.findByIdRecipeId(recipe.getId());
        BigDecimal totalCost = BigDecimal.ZERO;
        for (PreparationRecipeItem item : items) {
            BigDecimal rawCost = getMaterialCostPrice(item.getInputMaterial().getId());
            if (rawCost == null || rawCost.signum() <= 0) {
                throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Nguyên liệu thô '" + item.getInputMaterial().getName() + "' chưa có giá vốn nhập kho hợp lệ");
            }
            totalCost = totalCost.add(item.getStandardInputQuantity().multiply(rawCost));
        }

        return totalCost.divide(recipe.getStandardOutputQuantity(), 12, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateWeightedAverageUnitCost(
            Long outputMaterialId,
            Instant fromTime,
            Instant toTime,
            BigDecimal openingStock,
            BigDecimal openingCost) {
        List<PreparationBatch> batches;
        if (fromTime != null && toTime != null) {
            batches = batchRepository.findByOutputMaterialIdAndStatusAndPostedAtBetween(
                    outputMaterialId, DocumentStatus.DA_GHI_SO, fromTime, toTime);
        } else {
            batches = batchRepository.findByOutputMaterialIdAndStatus(outputMaterialId, DocumentStatus.DA_GHI_SO);
        }

        BigDecimal totalCost = (openingCost != null && openingCost.signum() > 0) ? openingCost : BigDecimal.ZERO;
        BigDecimal totalOutput = (openingStock != null && openingStock.signum() > 0) ? openingStock : BigDecimal.ZERO;

        for (PreparationBatch b : batches) {
            if (b.getTotalCost() != null && b.getTotalCost().signum() > 0) {
                totalCost = totalCost.add(b.getTotalCost());
            }
            if (b.getActualQuantity() != null && b.getActualQuantity().signum() > 0) {
                totalOutput = totalOutput.add(b.getActualQuantity());
            }
        }

        if (totalOutput.signum() <= 0 || totalCost.signum() <= 0) {
            // Fallback sang giá vốn tính theo định mức cố định hiện hành
            return calculateEstimatedRecipeUnitCost(outputMaterialId);
        }

        return totalCost.divide(totalOutput, 12, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public InventoryDto.ShiftSettlementPreviewResponse getShiftSettlementPreview(StockLocation location) {
        List<Stock> stocks = stockRepository.findByIdLocation(location);
        List<InventoryDto.ShiftSettlementPreviewItem> items = new ArrayList<>();

        for (Stock s : stocks) {
            Material m = s.getMaterial();
            if (m.getType() == MaterialType.SOCHE) {
                BigDecimal estimatedUnitCost = null;
                try {
                    estimatedUnitCost = calculateWeightedAverageUnitCost(m.getId(), null, null, null, null);
                } catch (Exception ignored) {
                }

                items.add(InventoryDto.ShiftSettlementPreviewItem.builder()
                        .materialId(m.getId())
                        .materialName(m.getName())
                        .unit(m.getUnit())
                        .bookQuantity(s.getQuantity())
                        .estimatedUnitCost(estimatedUnitCost)
                        .standardThreshold(s.getThreshold())
                        .build());
            }
        }

        return InventoryDto.ShiftSettlementPreviewResponse.builder()
                .location(location)
                .timestamp(Instant.now())
                .items(items)
                .build();
    }

    @Transactional
    public InventoryDto.ShiftSettlementResponse recordShiftSettlement(
            InventoryDto.CreateShiftSettlementRequest request,
            Account account,
            String idempotencyKey) {
        if (account == null) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn chưa đăng nhập");
        }

        String reqKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey;

        var existing = shiftSettlementRepository.findByIdempotencyKey(reqKey);
        if (existing.isPresent()) {
            return mapShiftSettlement(existing.get());
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Danh sách kiểm kê không được rỗng");
        }

        Instant now = Instant.now();
        ShiftSettlement settlement = ShiftSettlement.builder()
                .location(request.getLocation())
                .closedAt(now)
                .fromTime(now.minusSeconds(8 * 3600))
                .toTime(now)
                .closedBy(account)
                .notes(request.getNote())
                .idempotencyKey(reqKey)
                .build();
        settlement = shiftSettlementRepository.save(settlement);

        BigDecimal totalLossCost = BigDecimal.ZERO;
        List<ShiftSettlementItem> settlementItems = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        for (var itemReq : request.getItems()) {
            Material material = materialRepository.findById(itemReq.getMaterialId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nguyên liệu: ID " + itemReq.getMaterialId()));

            Stock stock = stockRepository.findByIdWithLock(material.getId(), request.getLocation())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.INVENTORY_INSUFFICIENT, "Không tìm thấy tồn kho: " + material.getName()));

            BigDecimal bookQty = stock.getQuantity();
            BigDecimal actualQty = itemReq.getActualQuantity();
            BigDecimal discardedQty = itemReq.getDiscardedQuantity();
            BigDecimal retainedQty = itemReq.getRetainedQuantity();

            if (discardedQty.compareTo(bookQty) > 0) {
                throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED,
                        "Lượng loại bỏ (" + discardedQty + ") không được vượt quá tồn sổ sách (" + bookQty + ") của " + material.getName());
            }

            BigDecimal diff = actualQty.subtract(bookQty);

            // Tính giá vốn bình quân theo sản lượng thực tế
            BigDecimal unitCost;
            try {
                unitCost = calculateWeightedAverageUnitCost(material.getId(), null, null, null, null);
            } catch (Exception ex) {
                unitCost = getMaterialCostPrice(material.getId());
                if (unitCost == null) {
                    throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                            "Chưa đủ dữ liệu giá vốn để tính hao hụt cho nguyên liệu: " + material.getName());
                }
            }

            BigDecimal lossCost = discardedQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            totalLossCost = totalLossCost.add(lossCost);

            // Giảm tồn đúng lượng loại bỏ (phần giữ lại được lưu tiếp trong kho)
            BigDecimal stockBefore = stock.getQuantity();
            BigDecimal stockAfter = stockBefore.subtract(discardedQty);
            stock.setQuantity(stockAfter);
            stockRepository.save(stock);

            if (discardedQty.signum() > 0) {
                movements.add(StockMovement.builder()
                        .material(material)
                        .location(request.getLocation())
                        .type(StockMovementType.HAO_HUT)
                        .deltaQuantity(discardedQty.negate())
                        .beforeQuantity(stockBefore)
                        .afterQuantity(stockAfter)
                        .sourceType("SHIFT_SETTLEMENT")
                        .sourceId(settlement.getId())
                        .sourceLine(material.getId())
                        .createdBy(account)
                        .createdAt(Instant.now())
                        .reason("Hao hụt loại bỏ cuối ca #" + settlement.getId() + (itemReq.getReason() != null ? ": " + itemReq.getReason() : ""))
                        .build());
            }

            settlementItems.add(ShiftSettlementItem.builder()
                    .settlement(settlement)
                    .material(material)
                    .unit(material.getUnit())
                    .bookQuantity(bookQty)
                    .actualQuantity(actualQty)
                    .discardedQuantity(discardedQty)
                    .retainedQuantity(retainedQty)
                    .unexplainedDifference(diff)
                    .unitCost(unitCost)
                    .lossCost(lossCost)
                    .reason(itemReq.getReason())
                    .build());
        }

        settlement.setTotalLossCost(totalLossCost);
        shiftSettlementRepository.save(settlement);
        shiftSettlementItemRepository.saveAll(settlementItems);
        if (!movements.isEmpty()) {
            movementRepository.saveAll(movements);
        }

        realtimeEventPublisher.publishAfterCommit(
                "/topic/stock",
                "STOCK_CHANGED",
                settlement.getId().toString(),
                null,
                "1",
                java.util.Map.of("action", "SHIFT_SETTLEMENT"));

        return mapShiftSettlement(settlement);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.ShiftSettlementResponse> listShiftSettlements(StockLocation location) {
        return listShiftSettlements(location, null, null, true);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto.ShiftSettlementResponse> listShiftSettlements(
            StockLocation location, LocalDate from, LocalDate to, boolean all) {
        List<ShiftSettlement> list;
        if (all) {
            list = (location != null)
                    ? shiftSettlementRepository.findByLocationOrderByClosedAtDesc(location)
                    : shiftSettlementRepository.findAllByOrderByClosedAtDesc();
        } else {
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDate startDate = (from != null) ? from : LocalDate.now(zone);
            LocalDate endDate = (to != null) ? to : LocalDate.now(zone);
            Instant startInstant = startDate.atStartOfDay(zone).toInstant();
            Instant endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant();

            list = (location != null)
                    ? shiftSettlementRepository.findByLocationAndClosedAtBetweenOrderByClosedAtDesc(location, startInstant, endInstant)
                    : shiftSettlementRepository.findByClosedAtBetweenOrderByClosedAtDesc(startInstant, endInstant);
        }
        return list.stream().map(this::mapShiftSettlement).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDto.ShiftSettlementResponse getShiftSettlement(Long id) {
        ShiftSettlement s = shiftSettlementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy biên bản kết ca: ID " + id));
        return mapShiftSettlement(s);
    }

    private InventoryDto.ShiftSettlementResponse mapShiftSettlement(ShiftSettlement settlement) {
        Account closer = settlement.getClosedBy();
        List<ShiftSettlementItem> items = shiftSettlementItemRepository.findBySettlementId(settlement.getId());

        List<InventoryDto.ShiftSettlementItemResponse> itemResponses = items.stream().map(it ->
                InventoryDto.ShiftSettlementItemResponse.builder()
                        .id(it.getId())
                        .materialId(it.getMaterial().getId())
                        .materialName(it.getMaterial().getName())
                        .unit(it.getUnit())
                        .bookQuantity(it.getBookQuantity())
                        .actualQuantity(it.getActualQuantity())
                        .discardedQuantity(it.getDiscardedQuantity())
                        .retainedQuantity(it.getRetainedQuantity())
                        .unexplainedDifference(it.getUnexplainedDifference())
                        .unitCost(it.getUnitCost())
                        .lossCost(it.getLossCost())
                        .reason(it.getReason())
                        .build()
        ).toList();

        return InventoryDto.ShiftSettlementResponse.builder()
                .id(settlement.getId())
                .location(settlement.getLocation())
                .closedAt(settlement.getClosedAt())
                .closedByEmail(closer != null ? closer.getEmail() : null)
                .closedByName(closer != null ? closer.getFullName() : null)
                .totalLossCost(settlement.getTotalLossCost())
                .note(settlement.getNotes())
                .items(itemResponses)
                .build();
    }
}
