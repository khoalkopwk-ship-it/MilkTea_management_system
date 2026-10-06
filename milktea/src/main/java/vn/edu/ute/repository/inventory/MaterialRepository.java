package vn.edu.ute.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ute.entity.inventory.Material;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByCode(String code);

    boolean existsByCode(String code);

    List<Material> findByActiveTrue();
}