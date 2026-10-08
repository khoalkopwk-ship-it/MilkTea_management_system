package vn.edu.ute.milktea.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.recipe.ProductRecipe;

import java.util.List;

@Repository
public interface ProductRecipeRepository extends JpaRepository<ProductRecipe, Object> {
    List<ProductRecipe> findByIdProductId(Long productId);
    List<ProductRecipe> findByIdProductIdIn(List<Long> productIds);
    void deleteByIdProductId(Long productId);
}
