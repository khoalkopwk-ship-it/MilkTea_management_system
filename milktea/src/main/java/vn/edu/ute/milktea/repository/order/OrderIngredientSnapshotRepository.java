package vn.edu.ute.milktea.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.order.OrderIngredientSnapshot;

import java.util.List;

@Repository
public interface OrderIngredientSnapshotRepository extends JpaRepository<OrderIngredientSnapshot, Object> {
    List<OrderIngredientSnapshot> findByIdOrderId(Long orderId);
}
