package vn.edu.ute.milktea.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.milktea.service.inventory.InventoryService;
import vn.edu.ute.milktea.repository.inventory.*;
import vn.edu.ute.milktea.repository.order.*;
import vn.edu.ute.milktea.repository.recipe.ProductRecipeRepository;
import vn.edu.ute.milktea.entity.inventory.*;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.entity.order.*;
import vn.edu.ute.milktea.entity.recipe.ProductRecipe;
import vn.edu.ute.milktea.common.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecipeConsumptionTest {
    @Mock StockRepository stockRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock OrderIngredientSnapshotRepository snapshotRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRecipeRepository productRecipeRepository;
    @InjectMocks InventoryService service;

    @Test void consumesCurrentRecipeTimesQuantityAndAggregatesSharedMaterials() {
        Order order = Order.builder().id(7L).build();
        Material raw = Material.builder().id(10L).name("Đường").type(MaterialType.THO).unit("kg").build();
        Material prepared = Material.builder().id(11L).name("Trân châu chín").type(MaterialType.SOCHE).unit("suất").build();

        OrderIngredientSnapshot snap1 = OrderIngredientSnapshot.builder()
                .material(raw)
                .totalQuantity(new BigDecimal("0.130"))
                .snapshotType(MaterialType.THO)
                .snapshotUnit("kg")
                .build();
        OrderIngredientSnapshot snap2 = OrderIngredientSnapshot.builder()
                .material(prepared)
                .totalQuantity(new BigDecimal("2.000"))
                .snapshotType(MaterialType.SOCHE)
                .snapshotUnit("suất")
                .build();

        when(snapshotRepository.findByIdOrderId(7L)).thenReturn(List.of(snap1, snap2));

        Stock rawStock = Stock.builder().id(new StockId(10L, StockLocation.KHO)).quantity(BigDecimal.ONE).build();
        Stock preparedStock = Stock.builder().id(new StockId(11L, StockLocation.BEP)).quantity(new BigDecimal("10")).build();

        when(stockRepository.findByIdWithLock(10L, StockLocation.KHO)).thenReturn(java.util.Optional.of(rawStock));
        when(stockRepository.findByIdWithLock(11L, StockLocation.BEP)).thenReturn(java.util.Optional.of(preparedStock));

        service.completeOrderConsumption(order, null);

        assertEquals(0, rawStock.getQuantity().compareTo(new BigDecimal("0.870")));
        assertEquals(0, preparedStock.getQuantity().compareTo(new BigDecimal("8.000")));
        verify(movementRepository).saveAll(argThat(m -> {
            var rows = (List<StockMovement>) m;
            return rows.size() == 2 && rows.get(0).getDeltaQuantity().compareTo(new BigDecimal("-0.130")) == 0;
        }));
    }

    @Test void missingRecipeDoesNotSilentlyCompleteWithoutConsumption() {
        when(snapshotRepository.findByIdOrderId(8L)).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.completeOrderConsumption(Order.builder().id(8L).build(), null));
        verifyNoInteractions(stockRepository, movementRepository);
    }
}
