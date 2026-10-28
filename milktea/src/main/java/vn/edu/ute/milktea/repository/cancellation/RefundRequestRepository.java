package vn.edu.ute.milktea.repository.cancellation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.cancellation.RefundRequest;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefundRequest r where r.id = :id")
    Optional<RefundRequest> findByIdWithLock(@Param("id") Long id);

    Optional<RefundRequest> findByOrderId(Long orderId);
    Optional<RefundRequest> findByPaymentId(Long paymentId);
    List<RefundRequest> findByStatus(RefundStatus status);

    @Query("SELECT r FROM RefundRequest r WHERE r.status = 'DA_HOAN' AND r.refundedAt >= :start AND r.refundedAt < :end")
    List<RefundRequest> findCompletedRefundsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT r FROM RefundRequest r
        JOIN r.order o
        WHERE r.status = vn.edu.ute.milktea.entity.cancellation.RefundStatus.DA_HOAN
          AND r.refundedAt >= :start AND r.refundedAt < :end
          AND (:branchId IS NULL OR o.branch.id = :branchId)
        """)
    List<RefundRequest> findCompletedRefundsBetweenAndBranchId(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("branchId") Long branchId
    );

    @Query("""
        SELECT r FROM RefundRequest r
        JOIN r.order o
        WHERE r.status = :status
          AND (:branchId IS NULL OR o.branch.id = :branchId)
        """)
    List<RefundRequest> findByStatusAndBranchId(
            @Param("status") RefundStatus status,
            @Param("branchId") Long branchId
    );

    @Query("""
        SELECT r FROM RefundRequest r
        JOIN r.order o
        WHERE :branchId IS NULL OR o.branch.id = :branchId
        """)
    List<RefundRequest> findByBranchId(@Param("branchId") Long branchId);
}

