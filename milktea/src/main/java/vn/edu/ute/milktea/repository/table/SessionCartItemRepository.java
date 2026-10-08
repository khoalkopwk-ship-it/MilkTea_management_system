package vn.edu.ute.milktea.repository.table;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.table.SessionCartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionCartItemRepository extends JpaRepository<SessionCartItem, Long> {
    List<SessionCartItem> findByCartId(Long cartId);
    Optional<SessionCartItem> findByCartIdAndProductId(Long cartId, Long productId);
    void deleteByCartId(Long cartId);
}
