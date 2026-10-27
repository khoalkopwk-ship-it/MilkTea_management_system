package vn.edu.ute.milktea.repository.voucher;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.voucher.Voucher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE LOWER(v.code) = LOWER(:code)")
    Optional<Voucher> findByCodeWithLock(@Param("code") String code);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :id AND v.usedCount < v.totalUsageLimit")
    int incrementUsageCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :id AND v.usedCount > 0")
    int decrementUsageCount(@Param("id") Long id);

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true AND v.startDate <= :now AND v.endDate >= :now AND v.usedCount < v.totalUsageLimit")
    List<Voucher> findActiveVouchers(@Param("now") Instant now);

    @Query("SELECT v FROM Voucher v WHERE (:keyword IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(v.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isActive IS NULL OR v.isActive = :isActive)")
    Page<Voucher> searchVouchers(@Param("keyword") String keyword, @Param("isActive") Boolean isActive, Pageable pageable);
}
