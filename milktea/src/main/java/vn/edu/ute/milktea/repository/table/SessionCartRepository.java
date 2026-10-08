package vn.edu.ute.milktea.repository.table;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.table.SessionCart;

import java.util.Optional;

@Repository
public interface SessionCartRepository extends JpaRepository<SessionCart, Long> {

    Optional<SessionCart> findBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SessionCart c WHERE c.session.id = :sessionId")
    Optional<SessionCart> findBySessionIdWithLock(@Param("sessionId") Long sessionId);

    void deleteBySessionId(Long sessionId);
}
