package vn.edu.ute.milktea.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.payment.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByInvoiceId(Long invoiceId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    boolean existsByInvoiceId(Long invoiceId);

    @Query("SELECT p FROM Payment p WHERE p.paidAt >= :start AND p.paidAt < :end")
    List<Payment> findByPaidAtBetween(@Param("start") Instant start, @Param("end") Instant end);
}
