package vn.edu.ute.milktea.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.payment.PaymentDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.payment.Payment;
import vn.edu.ute.milktea.entity.payment.PaymentNotice;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.payment.PaymentNoticeRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentNoticeRepository paymentNoticeRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public PaymentDto.PaymentResponse recordReceipt(Long invoiceId, PaymentDto.RecordPaymentRequest request, Long cashierId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Payment p = existing.get();
                return PaymentDto.PaymentResponse.builder()
                        .paymentId(p.getId())
                        .invoiceId(p.getInvoice().getId())
                        .method(p.getMethod())
                        .amount(p.getAmount())
                        .reference(p.getReference())
                        .recordedByEmail(p.getRecordedBy() != null ? p.getRecordedBy().getEmail() : null)
                        .paidAt(p.getPaidAt())
                        .build();
            }
        }

        Invoice invoice = invoiceRepository.findByIdWithLock(invoiceId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn"));

        if (invoice.getStatus() != InvoiceStatus.HIEU_LUC) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Hóa đơn đã bị hủy, không thể thu tiền");
        }

        if (paymentRepository.existsByInvoiceId(invoiceId)) {
            throw BusinessException.conflict(ErrorCode.PAYMENT_ALREADY_RECORDED, "Hóa đơn đã được thanh toán trước đó");
        }

        Account cashier = cashierId != null ? accountRepository.findById(cashierId).orElse(null) : null;

        Payment payment = Payment.builder()
                .invoice(invoice)
                .method(request.getMethod())
                .amount(invoice.getTotalAmount())
                .reference(request.getReference())
                .recordedBy(cashier)
                .paidAt(Instant.now())
                .idempotencyKey(idempotencyKey)
                .build();
        payment = paymentRepository.save(payment);

        auditRepository.save(BusinessAudit.builder()
                .orderId(invoice.getOrder().getId())
                .accountId(cashierId)
                .action("PAYMENT_RECORDED")
                .afterState("PAID")
                .reason("Ghi thu " + request.getMethod() + ": " + invoice.getTotalAmount() + " VND")
                .createdAt(Instant.now())
                .build());

        String sId = invoice.getOrder().getSession() != null ? invoice.getOrder().getSession().getId().toString() : null;
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "PAYMENT_RECORDED", payment.getId().toString(), sId, "1", java.util.Map.of("invoiceId", invoice.getId(), "amount", payment.getAmount()));
        if (invoice.getOrder().getSession() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + invoice.getOrder().getSession().getId(), "PAYMENT_RECORDED", payment.getId().toString(), sId, "1", java.util.Map.of("invoiceId", invoice.getId(), "amount", payment.getAmount()));
        }

        return PaymentDto.PaymentResponse.builder()
                .paymentId(payment.getId())
                .invoiceId(invoice.getId())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .reference(payment.getReference())
                .recordedByEmail(cashier != null ? cashier.getEmail() : null)
                .paidAt(payment.getPaidAt())
                .build();
    }

    @Transactional
    public void createNotice(Long invoiceId, PaymentDto.PaymentNoticeRequest request, String idempotencyKey) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn"));

        PaymentNotice notice = PaymentNotice.builder()
                .invoice(invoice)
                .method(request.getMethod())
                .reference(request.getReference())
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .build();
        paymentNoticeRepository.save(notice);

        auditRepository.save(BusinessAudit.builder()
                .orderId(invoice.getOrder().getId())
                .action("PAYMENT_NOTICE_CREATED")
                .reason("Khách báo đã chuyển khoản: " + request.getReference())
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "PAYMENT_NOTICE_CREATED", notice.getId().toString(), null, "1", java.util.Map.of("invoiceId", invoice.getId(), "method", request.getMethod()));
    }

    @Transactional
    public PaymentDto.PaymentResponse recordReceiptForOrder(Long orderId, PaymentDto.RecordPaymentRequest request, Long cashierId, String idempotencyKey) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn cho đơn hàng #" + orderId));
        return recordReceipt(invoice.getId(), request, cashierId, idempotencyKey);
    }

    @Transactional
    public void createNoticeForOrder(Long orderId, PaymentDto.PaymentNoticeRequest request, String idempotencyKey) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn cho đơn hàng #" + orderId));
        createNotice(invoice.getId(), request, idempotencyKey);
    }
}
