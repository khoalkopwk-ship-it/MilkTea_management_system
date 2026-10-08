package vn.edu.ute.milktea.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.milktea.entity.payment.PaymentNotice;

import java.util.List;

@Repository
public interface PaymentNoticeRepository extends JpaRepository<PaymentNotice, Long> {
    List<PaymentNotice> findByInvoiceId(Long invoiceId);
}
