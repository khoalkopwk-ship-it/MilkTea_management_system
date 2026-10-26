package vn.edu.ute.milktea.repository.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.catalog.ProductFavorite;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {

    boolean existsByAccountIdAndProductId(Long accountId, Long productId);

    Optional<ProductFavorite> findByAccountIdAndProductId(Long accountId, Long productId);

    Page<ProductFavorite> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    long countByProductId(Long productId);

    void deleteByAccountIdAndProductId(Long accountId, Long productId);

    @Query("SELECT pf.product.id FROM ProductFavorite pf GROUP BY pf.product.id ORDER BY COUNT(pf.id) DESC")
    List<Long> findMostFavoritedProductIds(Pageable pageable);
}
