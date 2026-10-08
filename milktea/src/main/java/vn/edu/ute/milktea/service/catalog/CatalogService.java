package vn.edu.ute.milktea.service.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.catalog.CatalogDto;
import vn.edu.ute.milktea.entity.catalog.Product;
import vn.edu.ute.milktea.repository.catalog.CategoryRepository;
import vn.edu.ute.milktea.repository.catalog.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CatalogDto.MenuResponse getMenu() {
        var categories = categoryRepository.findAllByActiveTrue();
        var allProducts = productRepository.findAllByActiveTrue();

        List<CatalogDto.CategoryItem> categoryItems = categories.stream().map(cat -> {
            var products = allProducts.stream()
                    .filter(p -> p.getCategory().getId().equals(cat.getId()))
                    .map(this::mapProduct)
                    .toList();

            return CatalogDto.CategoryItem.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .active(cat.getActive())
                    .products(products)
                    .build();
        }).toList();

        return CatalogDto.MenuResponse.builder()
                .categories(categoryItems)
                .build();
    }

    @Transactional(readOnly = true)
    public CatalogDto.ProductItem getProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PRODUCT_UNAVAILABLE, "Không tìm thấy món uống"));
        return mapProduct(p);
    }

    @Transactional(readOnly = true)
    public List<CatalogDto.CategoryItem> getCategories() {
        return categoryRepository.findAllByActiveTrue().stream().map(cat ->
                CatalogDto.CategoryItem.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .active(cat.getActive())
                        .build()
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogDto.ProductItem> getProducts() {
        return productRepository.findAllByActiveTrue().stream().map(this::mapProduct).toList();
    }

    private CatalogDto.ProductItem mapProduct(Product p) {
        return CatalogDto.ProductItem.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .name(p.getName())
                .size(p.getSize())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .active(p.getActive())
                .build();
    }
}
