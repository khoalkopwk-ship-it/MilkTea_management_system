package vn.edu.ute.service.inventory;

import org.springframework.stereotype.Service;

import vn.edu.ute.entity.inventory.Material;
import vn.edu.ute.entity.inventory.Stock;
import vn.edu.ute.entity.inventory.StockLocation;
import vn.edu.ute.repository.inventory.MaterialRepository;
import vn.edu.ute.repository.inventory.StockRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockAlertService {

    private final StockRepository stockRepository;
    private final MaterialRepository materialRepository;

    public StockAlertService(
            StockRepository stockRepository,
            MaterialRepository materialRepository
    ) {
        this.stockRepository = stockRepository;
        this.materialRepository = materialRepository;
    }

    public List<Stock> getLowStock(Long branchId) {

        if (branchId == null) {
            throw new IllegalArgumentException(
                    "branchId không được null"
            );
        }

        List<Stock> stocks =
                stockRepository.findByBranchId(branchId);

        List<Stock> lowStocks = new ArrayList<>();

        for (Stock stock : stocks) {

            Material material = stock.getMaterial();

            if (material == null) {
                continue;
            }

            if (Boolean.FALSE.equals(material.getActive())) {
                continue;
            }

            BigDecimal threshold =
                    material.getAlertThreshold();

            if (threshold == null) {
                threshold = BigDecimal.ZERO;
            }

            if (stock.getQuantity()
                    .compareTo(threshold) <= 0) {

                lowStocks.add(stock);
            }
        }

        return lowStocks;
    }

    public List<Stock> getLowKitchenStock(Long branchId) {

        if (branchId == null) {
            throw new IllegalArgumentException(
                    "branchId không được null"
            );
        }

        List<Stock> stocks =
                stockRepository.findByBranchIdAndLocation(
                        branchId,
                        StockLocation.BEP
                );

        List<Stock> lowStocks = new ArrayList<>();

        for (Stock stock : stocks) {

            Material material = stock.getMaterial();

            if (material == null) {
                continue;
            }

            if (Boolean.FALSE.equals(material.getActive())) {
                continue;
            }

            BigDecimal threshold =
                    material.getAlertThreshold();

            if (threshold == null) {
                threshold = BigDecimal.ZERO;
            }

            if (stock.getQuantity()
                    .compareTo(threshold) <= 0) {

                lowStocks.add(stock);
            }
        }

        return lowStocks;
    }

    public boolean isLowStock(
            Long branchId,
            Long materialId,
            StockLocation location
    ) {

        if (branchId == null) {
            throw new IllegalArgumentException(
                    "branchId không được null"
            );
        }

        if (materialId == null) {
            throw new IllegalArgumentException(
                    "materialId không được null"
            );
        }

        if (location == null) {
            throw new IllegalArgumentException(
                    "location không được null"
            );
        }

        Material material = materialRepository
                .findById(materialId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy nguyên liệu ID: "
                                        + materialId
                        )
                );

        Stock stock = stockRepository
                .findByBranchIdAndMaterialIdAndLocation(
                        branchId,
                        materialId,
                        location
                )
                .orElse(null);

        if (stock == null) {
            return true;
        }

        BigDecimal threshold =
                material.getAlertThreshold();

        if (threshold == null) {
            threshold = BigDecimal.ZERO;
        }

        return stock.getQuantity()
                .compareTo(threshold) <= 0;
    }
}