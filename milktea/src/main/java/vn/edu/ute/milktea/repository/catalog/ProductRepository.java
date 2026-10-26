package vn.edu.ute.milktea.repository.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.catalog.Product;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByActiveTrue();
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);
    boolean existsByNameIgnoreCaseAndSizeIgnoreCase(String name, String size);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByOrderByIdAsc();

    long countByImagePublicId(String imagePublicId);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findByIdInAndActiveTrue(java.util.Collection<Long> ids);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findByActiveTrueOrderByIdDesc(org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        """)
    org.springframework.data.domain.Page<Product> searchProducts(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("minPrice") java.math.BigDecimal minPrice,
            @org.springframework.data.repository.query.Param("maxPrice") java.math.BigDecimal maxPrice,
            org.springframework.data.domain.Pageable pageable
    );
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByCategoryAllowToppingFalseAndActiveTrue();

    @EntityGraph(attributePaths = {"category"})
    List<Product> findByNameIgnoreCaseAndActiveTrue(String name);
}


