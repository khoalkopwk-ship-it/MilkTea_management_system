package vn.edu.ute.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.order.Order;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndGuestToken(
            Long id,
            String guestToken
    );

    Optional<Order> findByOrderCode(String orderCode);
}