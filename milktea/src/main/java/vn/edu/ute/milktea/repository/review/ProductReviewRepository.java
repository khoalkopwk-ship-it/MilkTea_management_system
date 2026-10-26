package vn.edu.ute.milktea.repository.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.review.ProductReview;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    Page<ProductReview> findByProductIdAndHiddenFalseOrderByCreatedAtDesc(Long productId, Pageable pageable);

    long countByProductIdAndHiddenFalse(Long productId);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM ProductReview r WHERE r.product.id = :productId AND r.hidden = false")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    Page<ProductReview> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

    Page<ProductReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r.product.id FROM ProductReview r WHERE r.hidden = false GROUP BY r.product.id ORDER BY AVG(CAST(r.rating AS double)) DESC, COUNT(r.id) DESC")
    List<Long> findTopRatedProductIds(Pageable pageable);
}
