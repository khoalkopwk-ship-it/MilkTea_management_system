package vn.edu.ute.service.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.entity.inventory.*;
import vn.edu.ute.repository.inventory.*;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StockIssueService {

    private final MaterialRepository materialRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockIssueRepository stockIssueRepository;

    public StockIssueService(
            MaterialRepository materialRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            StockIssueRepository stockIssueRepository
    ) {
        this.materialRepository = materialRepository;
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockIssueRepository = stockIssueRepository;
    }

    @Transactional
    public StockIssue createStockIssue(
            Long branchId,
            String issueCode,
            Long createdBy,
            String note,
            List<StockIssueItem> items
    ) {

        if (branchId == null) {
            throw new IllegalArgumentException("branchId không được null");
        }

        if (issueCode == null || issueCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã phiếu xuất không được để trống"
            );
        }

        if (stockIssueRepository.existsByIssueCode(issueCode)) {
            throw new IllegalArgumentException(
                    "Mã phiếu xuất đã tồn tại: " + issueCode
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phiếu xuất phải có ít nhất một nguyên liệu"
            );
        }

        StockIssue issue = new StockIssue();
        issue.setBranchId(branchId);
        issue.setIssueCode(issueCode);
        issue.setCreatedBy(createdBy);
        issue.setNote(note);

        for (StockIssueItem item : items) {

            if (item.getMaterial() == null
                    || item.getMaterial().getId() == null) {
                throw new IllegalArgumentException(
                        "Nguyên liệu trong phiếu xuất không hợp lệ"
                );
            }

            if (item.getQuantity() == null
                    || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng xuất phải lớn hơn 0"
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
            issue.addItem(item);
        }

        StockIssue savedIssue =
                stockIssueRepository.save(issue);

        for (StockIssueItem item : savedIssue.getItems()) {

            moveWarehouseToKitchen(
                    branchId,
                    item.getMaterial(),
                    item.getQuantity(),
                    savedIssue.getId()
            );
        }

        return savedIssue;
    }

    private void moveWarehouseToKitchen(
            Long branchId,
            Material material,
            BigDecimal quantity,
            Long issueId
    ) {

        Stock warehouseStock = stockRepository
                .findByBranchIdAndMaterialIdAndLocation(
                        branchId,
                        material.getId(),
                        StockLocation.KHO
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tồn kho KHO của nguyên liệu: "
                                        + material.getName()
                        )
                );

        if (warehouseStock.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException(
                    "Tồn KHO không đủ cho nguyên liệu: "
                            + material.getName()
            );
        }

        Stock kitchenStock = stockRepository
                .findByBranchIdAndMaterialIdAndLocation(
                        branchId,
                        material.getId(),
                        StockLocation.BEP
                )
                .orElseGet(() -> {
                    Stock stock = new Stock();
                    stock.setBranchId(branchId);
                    stock.setMaterial(material);
                    stock.setLocation(StockLocation.BEP);
                    stock.setQuantity(BigDecimal.ZERO);
                    return stock;
                });

        warehouseStock.setQuantity(
                warehouseStock.getQuantity().subtract(quantity)
        );

        kitchenStock.setQuantity(
                kitchenStock.getQuantity().add(quantity)
        );

        stockRepository.save(warehouseStock);
        stockRepository.save(kitchenStock);

        StockMovement warehouseMovement =
                new StockMovement();

        warehouseMovement.setBranchId(branchId);
        warehouseMovement.setMaterial(material);
        warehouseMovement.setLocation(StockLocation.KHO);
        warehouseMovement.setMovementType(
                MovementType.ISSUE_OUT
        );
        warehouseMovement.setQuantity(
                quantity.negate()
        );
        warehouseMovement.setReferenceType(
                "STOCK_ISSUE"
        );
        warehouseMovement.setReferenceId(issueId);

        stockMovementRepository.save(warehouseMovement);

        StockMovement kitchenMovement =
                new StockMovement();

        kitchenMovement.setBranchId(branchId);
        kitchenMovement.setMaterial(material);
        kitchenMovement.setLocation(StockLocation.BEP);
        kitchenMovement.setMovementType(
                MovementType.ISSUE_IN
        );
        kitchenMovement.setQuantity(quantity);
        kitchenMovement.setReferenceType(
                "STOCK_ISSUE"
        );
        kitchenMovement.setReferenceId(issueId);

        stockMovementRepository.save(kitchenMovement);
    }
}