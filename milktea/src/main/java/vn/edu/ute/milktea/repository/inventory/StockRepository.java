package vn.edu.ute.milktea.repository.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.Stock;
import vn.edu.ute.milktea.entity.inventory.StockId;
import vn.edu.ute.milktea.entity.inventory.StockLocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {

    List<Stock> findByIdLocation(StockLocation location);

    Optional<Stock> findByIdMaterialIdAndIdLocation(Long materialId, StockLocation location);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id.location = :location AND s.id.materialId IN :materialIds ORDER BY s.id.materialId ASC")
    List<Stock> findByLocationAndMaterialIdInWithLock(@Param("location") StockLocation location,
                                                     @Param("materialIds") Collection<Long> materialIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id.materialId = :materialId AND s.id.location = :location")
    Optional<Stock> findByIdWithLock(@Param("materialId") Long materialId, @Param("location") StockLocation location);
}
