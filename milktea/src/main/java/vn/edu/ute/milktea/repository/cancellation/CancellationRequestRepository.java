package vn.edu.ute.milktea.repository.cancellation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.cancellation.CancellationRequest;
import vn.edu.ute.milktea.entity.cancellation.CancellationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, Long> {
    List<CancellationRequest> findByOrderId(Long orderId);
    Optional<CancellationRequest> findFirstByOrderIdAndStatus(Long orderId, CancellationStatus status);
    List<CancellationRequest> findByStatus(CancellationStatus status);
}
