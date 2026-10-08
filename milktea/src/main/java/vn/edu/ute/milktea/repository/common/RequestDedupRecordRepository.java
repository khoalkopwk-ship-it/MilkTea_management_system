package vn.edu.ute.milktea.repository.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.common.RequestDedupRecord;

import java.util.Optional;

@Repository
public interface RequestDedupRecordRepository extends JpaRepository<RequestDedupRecord, Long> {
    Optional<RequestDedupRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);
}
