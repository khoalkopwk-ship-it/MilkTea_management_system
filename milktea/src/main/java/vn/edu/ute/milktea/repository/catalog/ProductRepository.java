package vn.edu.ute.milktea.repository.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.catalog.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByActiveTrue();
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);
}
