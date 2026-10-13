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

}
