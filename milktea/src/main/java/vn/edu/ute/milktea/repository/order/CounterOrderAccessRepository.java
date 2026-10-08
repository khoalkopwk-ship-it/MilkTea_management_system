package vn.edu.ute.milktea.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.order.CounterOrderAccess;

import java.util.Optional;

@Repository
public interface CounterOrderAccessRepository extends JpaRepository<CounterOrderAccess, Long> {
    Optional<CounterOrderAccess> findByTokenHashAndRevokedAtIsNull(String tokenHash);
}
