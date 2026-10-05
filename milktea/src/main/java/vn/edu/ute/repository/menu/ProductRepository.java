package vn.edu.ute.repository.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.menu.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();
}