package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.Stock;
import vn.edu.ute.entity.inventory.StockLocation;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByBranchIdAndMaterialIdAndLocation(
            Long branchId,
            Long materialId,
            StockLocation location
    );

    List<Stock> findByBranchId(Long branchId);

    List<Stock> findByBranchIdAndLocation(
            Long branchId,
            StockLocation location
    );
}