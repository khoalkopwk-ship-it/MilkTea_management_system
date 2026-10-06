package vn.edu.ute.service.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.entity.inventory.*;
import vn.edu.ute.repository.inventory.*;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PreparationService {

    private final MaterialRepository materialRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PreparationBatchRepository preparationBatchRepository;

    public PreparationService(
            MaterialRepository materialRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            PreparationBatchRepository preparationBatchRepository
    ) {
        this.materialRepository = materialRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.preparationBatchRepository = preparationBatchRepository;
    }

    @Transactional
    public PreparationBatch createBatch(
            Long branchId,
            String batchCode,
            Long createdBy,
            String note,
            List<PreparationBatchItem> items
    ) {

        if (branchId == null) {
            throw new IllegalArgumentException("branchId không được null");
        }

        if (batchCode == null || batchCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã mẻ sơ chế không được để trống"
            );
        }

        if (preparationBatchRepository.existsByBatchCode(batchCode)) {
            throw new IllegalArgumentException(
                    "Mã mẻ sơ chế đã tồn tại: " + batchCode
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mẻ sơ chế phải có nguyên liệu"
            );
        }

        PreparationBatch batch = new PreparationBatch();
        batch.setBranchId(branchId);
        batch.setBatchCode(batchCode);
        batch.setCreatedBy(createdBy);
        batch.setNote(note);

        for (PreparationBatchItem item : items) {

            if (item.getMaterial() == null
                    || item.getMaterial().getId() == null) {
                throw new IllegalArgumentException(
                        "Nguyên liệu của mẻ không hợp lệ"
                );
            }

            if (item.getQuantity() == null
                    || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng phải lớn hơn 0"
                );
            }

            if (item.getItemRole() == null) {
                throw new IllegalArgumentException(
                        "Phải xác định INPUT hoặc OUTPUT"
                );
            }

            Material material = materialRepository
                    .findById(item.getMaterial().getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không tìm thấy nguyên liệu ID: "
                                            + item.getMaterial().getId()
                            )
                    );

            item.setMaterial(material);
            batch.addItem(item);
        }

        validateInputs(branchId, batch.getItems());

        PreparationBatch savedBatch =
                preparationBatchRepository.save(batch);

        for (PreparationBatchItem item : savedBatch.getItems()) {

            if (item.getItemRole() == PreparationItemRole.INPUT) {
                consumeInput(
                        branchId,
                        item.getMaterial(),
                        item.getQuantity(),
                        savedBatch.getId()
                );
            } else {
                produceOutput(
                        branchId,
                        item.getMaterial(),
                        item.getQuantity(),
                        savedBatch.getId()
                );
            }
        }

        return savedBatch;
    }

    private void validateInputs(
            Long branchId,
            List<PreparationBatchItem> items
    ) {

        for (PreparationBatchItem item : items) {

            if (item.getItemRole() != PreparationItemRole.INPUT) {
                continue;
            }

            Stock stock = stockRepository
                    .findByBranchIdAndMaterialIdAndLocation(
                            branchId,
                            item.getMaterial().getId(),
                            StockLocation.BEP
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không có tồn BẾP cho nguyên liệu: "
                                            + item.getMaterial().getName()
                            )
                    );

            if (stock.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw new IllegalArgumentException(
                        "Tồn BẾP không đủ cho nguyên liệu: "
                                + item.getMaterial().getName()
                );
            }
        }
    }

    private void consumeInput(
            Long branchId,
            Material material,
            BigDecimal quantity,
            Long batchId
    ) {

        Stock stock = stockRepository
                .findByBranchIdAndMaterialIdAndLocation(
                        branchId,
                        material.getId(),
                        StockLocation.BEP
                )
                .orElseThrow();

        stock.setQuantity(
                stock.getQuantity().subtract(quantity)
        );

        stockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setBranchId(branchId);
        movement.setMaterial(material);
        movement.setLocation(StockLocation.BEP);
        movement.setMovementType(
                MovementType.PREPARATION_INPUT
        );
        movement.setQuantity(quantity.negate());
        movement.setReferenceType("PREPARATION_BATCH");
        movement.setReferenceId(batchId);

        stockMovementRepository.save(movement);
    }

    private void produceOutput(
            Long branchId,
            Material material,
            BigDecimal quantity,
            Long batchId
    ) {

        Stock stock = stockRepository
                .findByBranchIdAndMaterialIdAndLocation(
                        branchId,
                        material.getId(),
                        StockLocation.BEP
                )
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setBranchId(branchId);
                    newStock.setMaterial(material);
                    newStock.setLocation(StockLocation.BEP);
                    newStock.setQuantity(BigDecimal.ZERO);
                    return newStock;
                });

        stock.setQuantity(
                stock.getQuantity().add(quantity)
        );

        stockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setBranchId(branchId);
        movement.setMaterial(material);
        movement.setLocation(StockLocation.BEP);
        movement.setMovementType(
                MovementType.PREPARATION_OUTPUT
        );
        movement.setQuantity(quantity);
        movement.setReferenceType("PREPARATION_BATCH");
        movement.setReferenceId(batchId);

        stockMovementRepository.save(movement);
    }
}