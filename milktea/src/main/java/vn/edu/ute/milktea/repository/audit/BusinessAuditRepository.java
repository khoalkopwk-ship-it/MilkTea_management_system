package vn.edu.ute.milktea.repository.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;

import java.util.List;

@Repository
public interface BusinessAuditRepository extends JpaRepository<BusinessAudit, Long> {
    List<BusinessAudit> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<BusinessAudit> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    List<BusinessAudit> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<BusinessAudit> findTop100ByOrderByCreatedAtDesc();
}
