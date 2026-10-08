package vn.edu.ute.milktea.repository.table;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.table.TableSession;
import vn.edu.ute.milktea.entity.table.TableSessionStatus;

import java.util.Optional;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    Optional<TableSession> findTopByTableIdAndStatusOrderByOpenedAtDesc(Long tableId, TableSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TableSession s WHERE s.id = :id")
    Optional<TableSession> findByIdWithLock(@Param("id") Long id);
}
