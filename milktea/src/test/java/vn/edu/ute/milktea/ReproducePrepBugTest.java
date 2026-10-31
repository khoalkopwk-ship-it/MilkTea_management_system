package vn.edu.ute.milktea;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.edu.ute.milktea.dto.InventoryDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.recipe.PreparationRecipeRepository;
import vn.edu.ute.milktea.service.inventory.InventoryService;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
public class ReproducePrepBugTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PreparationRecipeRepository preparationRecipeRepository;

    @Test
    void testRecordPreparationAllRecipes() {
        Account kitchen = accountRepository.findById(8L).orElseThrow(); // kitchen@milktea.vn, CN01
        var recipes = preparationRecipeRepository.findAllByActiveTrue();
        System.out.println("Found active recipes: " + recipes.size());

        for (var recipe : recipes) {
            System.out.println("Testing recipe: ID " + recipe.getId() + " - Name: " + recipe.getName() + " - OutputMat: " + recipe.getOutputMaterial().getId());
            InventoryDto.CreatePreparationBatchRequest req = new InventoryDto.CreatePreparationBatchRequest();
            req.setOutputMaterialId(recipe.getOutputMaterial().getId());
            req.setRecipeVersion(recipe.getVersion());
            req.setDesiredQuantity(BigDecimal.valueOf(100));
            req.setBranchId(1L);

            try {
                inventoryService.recordPreparation(req, kitchen, "TEST-PREP-" + UUID.randomUUID());
                System.out.println("SUCCESS for recipe " + recipe.getId());
            } catch (Exception e) {
                System.out.println("FAILED for recipe " + recipe.getId() + ": " + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
}
