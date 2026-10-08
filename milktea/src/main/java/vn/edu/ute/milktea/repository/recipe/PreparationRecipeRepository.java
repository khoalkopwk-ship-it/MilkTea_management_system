package vn.edu.ute.milktea.repository.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.recipe.PreparationRecipe;

import java.util.List;

@Repository
public interface PreparationRecipeRepository extends JpaRepository<PreparationRecipe, Long> {
    List<PreparationRecipe> findAllByActiveTrue();
}
