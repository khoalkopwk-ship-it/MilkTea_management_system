package vn.edu.ute.repository.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.menu.ProductSize;

import java.util.List;
import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

    List<ProductSize> findByProductIdAndActiveTrue(Long productId);

    Optional<ProductSize> findByIdAndProductIdAndActiveTrue(
            Long id,
            Long productId
    );
}