package vn.edu.ute.milktea.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.inventory.Material;
import vn.edu.ute.milktea.entity.inventory.MaterialType;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByActiveTrue();
    List<Material> findByTypeAndActiveTrue(MaterialType type);
}
