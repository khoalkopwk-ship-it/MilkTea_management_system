package vn.edu.ute.milktea.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.order.OrderItem;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    // Dashboard: Top món bán chạy, không tính đơn bị hủy
    @Query("""
        SELECT MIN(oi.product.id),
               oi.snapshotName,
               SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.createdAt >= :start
          AND o.createdAt < :end
          AND o.status <> :cancelledStatus
        GROUP BY oi.snapshotName
        ORDER BY SUM(oi.quantity) DESC, oi.snapshotName ASC
        """)
    List<Object[]> findTopSellingProducts(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("cancelledStatus")
            vn.edu.ute.milktea.entity.order.OrderStatus cancelledStatus,
            Pageable pageable
    );

    @Query("""
        SELECT oi.product.id
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = vn.edu.ute.milktea.entity.order.OrderStatus.HOAN_THANH
          AND (:branchId IS NULL OR o.branch.id = :branchId)
          AND EXISTS (SELECT p FROM vn.edu.ute.milktea.entity.payment.Payment p JOIN p.invoice i WHERE i.order.id = o.id)
        GROUP BY oi.product.id
        ORDER BY SUM(oi.quantity) DESC
        """)
    List<Long> findBestSellerProductIds(@Param("branchId") Long branchId, Pageable pageable);
}
