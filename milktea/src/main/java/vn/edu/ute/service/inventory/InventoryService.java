package vn.edu.ute.service.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.ute.dto.inventory.OrderConsumptionItem;
import vn.edu.ute.dto.inventory.OrderConsumptionRequest;
import vn.edu.ute.entity.inventory.*;
import vn.edu.ute.repository.inventory.*;

import java.math.BigDecimal;
import java.util.List;
import vn.edu.ute.dto.inventory.StockResponse;
import java.util.ArrayList;

@Service
public class InventoryService {

        private final MaterialRepository materialRepository;
        private final StockRepository stockRepository;
        private final StockMovementRepository stockMovementRepository;
        private final ImportReceiptRepository importReceiptRepository;

        public InventoryService(
                        MaterialRepository materialRepository,
                        StockRepository stockRepository,
                        StockMovementRepository stockMovementRepository,
                        ImportReceiptRepository importReceiptRepository) {
                this.materialRepository = materialRepository;
                this.stockRepository = stockRepository;
                this.stockMovementRepository = stockMovementRepository;
                this.importReceiptRepository = importReceiptRepository;
        }

        @Transactional
        public ImportReceipt createImportReceipt(
                        Long branchId,
                        String receiptCode,
                        Long createdBy,
                        String note,
                        List<ImportReceiptItem> items) {
                if (branchId == null) {
                        throw new IllegalArgumentException(
                                        "branchId không được null");
                }

                if (receiptCode == null || receiptCode.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Mã phiếu nhập không được để trống");
                }

                if (importReceiptRepository.existsByReceiptCode(receiptCode)) {
                        throw new IllegalArgumentException(
                                        "Mã phiếu nhập đã tồn tại: " + receiptCode);
                }

                if (items == null || items.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Phiếu nhập phải có ít nhất một nguyên liệu");
                }

                ImportReceipt receipt = new ImportReceipt();

                receipt.setBranchId(branchId);
                receipt.setReceiptCode(receiptCode);
                receipt.setCreatedBy(createdBy);
                receipt.setNote(note);

                for (ImportReceiptItem item : items) {

                        if (item.getMaterial() == null
                                        || item.getMaterial().getId() == null) {

                                throw new IllegalArgumentException(
                                                "Nguyên liệu trong phiếu nhập không hợp lệ");
                        }

                        if (item.getQuantity() == null
                                        || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {

                                throw new IllegalArgumentException(
                                                "Số lượng nhập phải lớn hơn 0");
                        }

                        Material material = materialRepository
                                        .findById(item.getMaterial().getId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Không tìm thấy nguyên liệu ID: "
                                                                        + item.getMaterial().getId()));

                        item.setMaterial(material);

                        receipt.addItem(item);
                }

                ImportReceipt savedReceipt = importReceiptRepository.save(receipt);

                for (ImportReceiptItem item : savedReceipt.getItems()) {

                        increaseWarehouseStock(
                                        branchId,
                                        item.getMaterial(),
                                        item.getQuantity(),
                                        savedReceipt.getId());
                }

                return savedReceipt;
        }

        @Transactional
        public void completeOrderConsumption(
                        OrderConsumptionRequest request) {
                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Yêu cầu tiêu hao không được null");
                }

                if (request.getOrderId() == null) {
                        throw new IllegalArgumentException(
                                        "orderId không được null");
                }

                if (request.getBranchId() == null) {
                        throw new IllegalArgumentException(
                                        "branchId không được null");
                }

                if (request.getItems() == null
                                || request.getItems().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Đơn hàng không có định mức tiêu hao");
                }

                boolean alreadyConsumed = stockMovementRepository
                                .existsByReferenceTypeAndReferenceId(
                                                "ORDER",
                                                request.getOrderId());

                if (alreadyConsumed) {
                        return;
                }

                validateOrderConsumption(request);

                for (OrderConsumptionItem item : request.getItems()) {

                        consumeOrderItem(
                                        request.getBranchId(),
                                        request.getOrderId(),
                                        item);
                }
        }

        private void increaseWarehouseStock(
                        Long branchId,
                        Material material,
                        BigDecimal quantity,
                        Long receiptId) {
                Stock stock = stockRepository
                                .findByBranchIdAndMaterialIdAndLocation(
                                                branchId,
                                                material.getId(),
                                                StockLocation.KHO)
                                .orElseGet(() -> {

                                        Stock newStock = new Stock();

                                        newStock.setBranchId(branchId);
                                        newStock.setMaterial(material);
                                        newStock.setLocation(StockLocation.KHO);
                                        newStock.setQuantity(BigDecimal.ZERO);

                                        return newStock;
                                });

                stock.setQuantity(
                                stock.getQuantity().add(quantity));

                stockRepository.save(stock);

                StockMovement movement = new StockMovement();

                movement.setBranchId(branchId);
                movement.setMaterial(material);
                movement.setLocation(StockLocation.KHO);
                movement.setMovementType(MovementType.IMPORT);
                movement.setQuantity(quantity);
                movement.setReferenceType("IMPORT_RECEIPT");
                movement.setReferenceId(receiptId);

                stockMovementRepository.save(movement);
        }

        private void validateOrderConsumption(
                        OrderConsumptionRequest request) {
                for (OrderConsumptionItem item : request.getItems()) {

                        if (item.getMaterialId() == null) {
                                throw new IllegalArgumentException(
                                                "materialId không được null");
                        }

                        if (item.getQuantity() == null
                                        || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {

                                throw new IllegalArgumentException(
                                                "Số lượng tiêu hao phải lớn hơn 0");
                        }

                        Material material = materialRepository
                                        .findById(item.getMaterialId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Không tìm thấy nguyên liệu ID: "
                                                                        + item.getMaterialId()));

                        Stock stock = stockRepository
                                        .findByBranchIdAndMaterialIdAndLocation(
                                                        request.getBranchId(),
                                                        material.getId(),
                                                        StockLocation.BEP)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Không có tồn BẾP cho nguyên liệu: "
                                                                        + material.getName()));

                        if (stock.getQuantity()
                                        .compareTo(item.getQuantity()) < 0) {

                                throw new IllegalArgumentException(
                                                "Tồn BẾP không đủ cho nguyên liệu: "
                                                                + material.getName());
                        }
                }
        }

        private void consumeOrderItem(
                        Long branchId,
                        Long orderId,
                        OrderConsumptionItem item) {
                Material material = materialRepository
                                .findById(item.getMaterialId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không tìm thấy nguyên liệu ID: "
                                                                + item.getMaterialId()));

                Stock stock = stockRepository
                                .findByBranchIdAndMaterialIdAndLocation(
                                                branchId,
                                                material.getId(),
                                                StockLocation.BEP)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Không có tồn BẾP cho nguyên liệu: "
                                                                + material.getName()));

                stock.setQuantity(
                                stock.getQuantity()
                                                .subtract(item.getQuantity()));

                stockRepository.save(stock);

                StockMovement movement = new StockMovement();

                movement.setBranchId(branchId);
                movement.setMaterial(material);
                movement.setLocation(StockLocation.BEP);
                movement.setMovementType(
                                MovementType.CONSUMPTION);

                movement.setQuantity(
                                item.getQuantity().negate());

                movement.setReferenceType("ORDER");
                movement.setReferenceId(orderId);

                stockMovementRepository.save(movement);
        }
        public List<StockResponse> getStocks(Long branchId) {

                if (branchId == null) {
                        throw new IllegalArgumentException(
                                "branchId không được null"
                        );
                }

                List<Stock> stocks =
                        stockRepository.findByBranchId(branchId);

                List<StockResponse> responses =
                        new ArrayList<>();

                for (Stock stock : stocks) {

                        StockResponse response =
                                new StockResponse();

                        response.setStockId(stock.getId());
                        response.setMaterialId(
                                stock.getMaterial().getId()
                        );
                        response.setMaterialCode(
                                stock.getMaterial().getCode()
                        );
                        response.setMaterialName(
                                stock.getMaterial().getName()
                        );
                        response.setMaterialType(
                                stock.getMaterial()
                                        .getType()
                                        .name()
                        );
                        response.setUnit(
                                stock.getMaterial().getUnit()
                        );
                        response.setLocation(
                                stock.getLocation().name()
                        );
                        response.setQuantity(
                                stock.getQuantity()
                        );
                        response.setAlertThreshold(
                                stock.getMaterial()
                                        .getAlertThreshold()
                        );

                        BigDecimal threshold =
                                stock.getMaterial().getAlertThreshold();

                        if (threshold == null) {
                        threshold = BigDecimal.ZERO;
                        }

                        response.setLowStock(
                                stock.getQuantity()
                                        .compareTo(threshold) <= 0
                        );

                        responses.add(response);
                }

                return responses;
        }
}