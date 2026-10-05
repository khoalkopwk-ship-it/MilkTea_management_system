package vn.edu.ute.service.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.dto.customer.MenuItemResponse;
import vn.edu.ute.dto.customer.MenuSizeResponse;
import vn.edu.ute.repository.menu.ProductRepository;
import vn.edu.ute.repository.menu.ProductSizeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenu() {

        return productRepository
                .findByActiveTrue()
                .stream()
                .map(product -> {

                    List<MenuSizeResponse> sizes =
                            productSizeRepository
                                    .findByProductIdAndActiveTrue(
                                            product.getId()
                                    )
                                    .stream()
                                    .map(size ->
                                            MenuSizeResponse.builder()
                                                    .id(size.getId())
                                                    .name(size.getName())
                                                    .extraPrice(
                                                            size.getExtraPrice()
                                                    )
                                                    .build()
                                    )
                                    .toList();

                    return MenuItemResponse.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .description(
                                    product.getDescription()
                            )
                            .basePrice(
                                    product.getBasePrice()
                            )
                            .imageUrl(
                                    product.getImageUrl()
                            )
                            .sizes(sizes)
                            .build();
                })
                .toList();
    }
}