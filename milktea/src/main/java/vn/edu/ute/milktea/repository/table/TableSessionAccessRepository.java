package vn.edu.ute.milktea.repository.table;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.table.TableSessionAccess;

import java.util.Optional;

@Repository
public interface TableSessionAccessRepository extends JpaRepository<TableSessionAccess, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"session", "session.table"})
    Optional<TableSessionAccess> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    void deleteBySessionId(Long sessionId);
}
