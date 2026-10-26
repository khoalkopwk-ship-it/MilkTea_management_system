package vn.edu.ute.milktea.repository.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findBySessionId(Long sessionId);

    List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<Order> findByBranchIdAndStatusInOrderByCreatedAtAsc(Long branchId, Collection<OrderStatus> statuses);

    List<Order> findByBranchIdAndSessionId(Long branchId, Long sessionId);

    List<Order> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.session.id = :sessionId AND o.status NOT IN ('HOAN_THANH', 'DA_HUY')")
    long countUnfinishedOrdersBySessionId(@Param("sessionId") Long sessionId);
    
    // Dashboard: thống kê số đơn theo trạng thái trong kỳ
    @Query("""
        SELECT o.status, COUNT(o)
        FROM Order o
        WHERE o.createdAt >= :start
          AND o.createdAt < :end
        GROUP BY o.status
        """)
    List<Object[]> countOrdersByStatusBetween(
            @Param("start") Instant start,
            @Param("end") Instant end
    );

}
