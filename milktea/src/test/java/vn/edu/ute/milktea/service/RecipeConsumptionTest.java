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
        Product a = Product.builder().id(1L).build(), b = Product.builder().id(2L).build();
        Material raw = Material.builder().id(10L).name("Đường").unit("kg").build();
        Material prepared = Material.builder().id(11L).name("Trân châu chín").unit("suất").build();
        when(orderItemRepository.findByOrderId(7L)).thenReturn(List.of(
                OrderItem.builder().product(a).quantity(2).build(),
                OrderItem.builder().product(b).quantity(3).build()));
        when(productRecipeRepository.findByIdProductId(1L)).thenReturn(List.of(
                ProductRecipe.builder().material(raw).quantity(new BigDecimal("0.020")).build(),
                ProductRecipe.builder().material(prepared).quantity(BigDecimal.ONE).build()));
        when(productRecipeRepository.findByIdProductId(2L)).thenReturn(List.of(
                ProductRecipe.builder().material(raw).quantity(new BigDecimal("0.030")).build()));
        when(snapshotRepository.findByIdOrderId(7L)).thenReturn(List.of(
                OrderIngredientSnapshot.builder().material(raw).totalQuantity(new BigDecimal("99")).build()));
        Stock rawStock = Stock.builder().id(new StockId(10L, StockLocation.BEP)).quantity(BigDecimal.ONE).build();
        Stock preparedStock = Stock.builder().id(new StockId(11L, StockLocation.BEP)).quantity(new BigDecimal("10")).build();
        when(stockRepository.findByLocationAndMaterialIdInWithLock(StockLocation.BEP, List.of(10L,11L)))
                .thenReturn(List.of(rawStock, preparedStock));
        service.completeOrderConsumption(order, null);
        assertEquals(0, rawStock.getQuantity().compareTo(new BigDecimal("0.870")));
        assertEquals(0, preparedStock.getQuantity().compareTo(new BigDecimal("8")));
        verify(movementRepository).saveAll(argThat(m -> {
            var rows = (List<StockMovement>)m;
            return rows.size() == 2 && rows.get(0).getDeltaQuantity().compareTo(new BigDecimal("-0.130")) == 0;
        }));
    }

    @Test void missingRecipeDoesNotSilentlyCompleteWithoutConsumption() {
        when(orderItemRepository.findByOrderId(8L)).thenReturn(List.of(
                OrderItem.builder().product(Product.builder().id(3L).build()).quantity(1).snapshotName("Món thiếu công thức").build()));
        when(productRecipeRepository.findByIdProductId(3L)).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.completeOrderConsumption(Order.builder().id(8L).build(), null));
        verifyNoInteractions(stockRepository, movementRepository);
    }
}
