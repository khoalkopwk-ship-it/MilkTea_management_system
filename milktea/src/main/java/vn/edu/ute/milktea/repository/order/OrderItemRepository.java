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

    // Dashboard: Top món bán chạy, chỉ tính đơn hoàn thành và đã thanh toán (F24)
    @Query("""
        SELECT oi.product.id,
               oi.product.name,
               SUM(oi.quantity)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.createdAt >= :start
          AND o.createdAt < :end
          AND o.status = vn.edu.ute.milktea.entity.order.OrderStatus.HOAN_THANH
          AND EXISTS (SELECT p FROM vn.edu.ute.milktea.entity.payment.Payment p JOIN p.invoice i WHERE i.order.id = o.id)
          AND (:branchId IS NULL OR o.branch.id = :branchId)
        GROUP BY oi.product.id, oi.product.name
        ORDER BY SUM(oi.quantity) DESC, oi.product.name ASC
        """)
    List<Object[]> findTopSellingProducts(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("branchId") Long branchId,
            Pageable pageable
    );

    default List<Object[]> findTopSellingProducts(
            Instant start,
            Instant end,
            Pageable pageable
    ) {
        return findTopSellingProducts(start, end, null, pageable);
    }

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
