package vn.edu.ute.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.order.OrderItem;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
}