package vn.edu.ute.milktea.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.order.OrderItemTopping;

import java.util.List;

@Repository
public interface OrderItemToppingRepository extends JpaRepository<OrderItemTopping, Long> {
    List<OrderItemTopping> findByOrderItemId(Long orderItemId);
    List<OrderItemTopping> findByOrderItemOrderId(Long orderId);
}
