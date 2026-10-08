package vn.edu.ute.milktea.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipeItem;

import java.util.List;

@Repository
public interface PreparationRecipeItemRepository extends JpaRepository<PreparationRecipeItem, Object> {
    List<PreparationRecipeItem> findByIdRecipeId(Long recipeId);
    void deleteByIdRecipeId(Long recipeId);
}
