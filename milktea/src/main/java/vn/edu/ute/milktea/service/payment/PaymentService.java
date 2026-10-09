package vn.edu.ute.milktea.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.PaymentDto;
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
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderSource;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentNoticeRepository paymentNoticeRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    
    @Transactional
    public PaymentDto.PaymentResponse recordReceipt(
            Long invoiceId,
            PaymentDto.RecordPaymentRequest request,
            Long cashierId,
            String idempotencyKey) {

        // 1. Kiểm tra dữ liệu đầu vào
        if (request == null || request.getMethod() == null) {
            throw BusinessException.conflict(
                    ErrorCode.VALIDATION_FAILED,
                    "Phương thức thanh toán không hợp lệ");
        }

        if (idempotencyKey != null
                && idempotencyKey.length() > 64) {
            throw BusinessException.conflict(
                    ErrorCode.VALIDATION_FAILED,
                    "Mã yêu cầu thanh toán quá dài");
        }

        // 2. Đọc hóa đơn để xác định đơn hàng
        Invoice invoiceLookup = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy hóa đơn"));

        // 3. Khóa Order trước, sau đó khóa Invoice
        Order order = orderRepository.findByIdWithLock(
                        invoiceLookup.getOrder().getId())
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy đơn hàng"));

        Invoice invoice = invoiceRepository.findByIdWithLock(invoiceId)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy hóa đơn"));

        // 4. Xử lý request gửi lại với cùng idempotency key
        if (idempotencyKey != null
                && !idempotencyKey.isBlank()) {

            var existing = paymentRepository
                    .findByIdempotencyKey(idempotencyKey);

            if (existing.isPresent()) {
                Payment oldPayment = existing.get();

                // Không cho tái sử dụng key của hóa đơn khác
                if (!oldPayment.getInvoice().getId()
                        .equals(invoiceId)) {
                    throw BusinessException.conflict(
                            ErrorCode.IDEMPOTENCY_CONFLICT,
                            "Mã yêu cầu đã được dùng cho hóa đơn khác");
                }

                // Không cho đổi phương thức trong cùng một key
                if (oldPayment.getMethod() != request.getMethod()) {
                    throw BusinessException.conflict(
                            ErrorCode.IDEMPOTENCY_CONFLICT,
                            "Mã yêu cầu đã được dùng với phương thức khác");
                }

                return toPaymentResponse(oldPayment);
            }
        }

        // 5. Kiểm tra trạng thái hóa đơn
        if (invoice.getStatus() != InvoiceStatus.HIEU_LUC) {
            throw BusinessException.conflict(
                    ErrorCode.ORDER_STATE_CONFLICT,
                    "Hóa đơn đã bị hủy, không thể thu tiền");
        }

        // 6. Chỉ thu tiền khi đơn đang chờ thanh toán
        if (order.getStatus() != OrderStatus.CHO_THANH_TOAN) {
            throw BusinessException.conflict(
                    ErrorCode.ORDER_STATE_CONFLICT,
                    "Đơn hàng không ở trạng thái chờ thanh toán");
        }

        // 7. Quy tắc phương thức thanh toán
        if (order.getSource() == OrderSource.TABLE
                && request.getMethod() == PaymentMethod.CASH) {
            throw BusinessException.conflict(
                    ErrorCode.VALIDATION_FAILED,
                    "Đơn tại bàn chỉ được chuyển khoản");
        }

        // 8. Không ghi khoản thu thứ hai cho cùng hóa đơn
        if (paymentRepository.existsByInvoiceId(invoiceId)) {
            throw BusinessException.conflict(
                    ErrorCode.PAYMENT_ALREADY_RECORDED,
                    "Hóa đơn đã được thanh toán");
        }

        // 9. Kiểm tra số tiền hóa đơn
        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().signum() <= 0) {
            throw BusinessException.conflict(
                    ErrorCode.VALIDATION_FAILED,
                    "Số tiền phải trả không hợp lệ");
        }

        // 10. Xác định nhân viên thu ngân
        if (cashierId == null) {
            throw BusinessException.conflict(
                    ErrorCode.ACCESS_DENIED,
                    "Chưa xác định nhân viên thu tiền");
        }

        Account cashier = accountRepository.findById(cashierId)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy nhân viên thu tiền"));

        // 11. Tạo Payment bằng số tiền server đã chốt
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

        // 12. Chuyển trạng thái trong cùng transaction
        order.setStatus(OrderStatus.CHO_XAC_NHAN);
        orderRepository.save(order);

        // 13. Ghi lịch sử nghiệp vụ
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(cashierId)
                .action("PAYMENT_RECORDED")
                .beforeState("CHO_THANH_TOAN")
                .afterState("CHO_XAC_NHAN")
                .reason("Ghi thu " + request.getMethod()
                        + ": " + payment.getAmount() + " VND")
                .createdAt(Instant.now())
                .build());

        // 14. Phát sự kiện sau khi transaction commit
        String sessionId = order.getSession() != null
                ? order.getSession().getId().toString()
                : null;

        java.util.Map<String, Object> eventData =
                java.util.Map.of(
                        "orderId", order.getId(),
                        "invoiceId", invoice.getId(),
                        "amount", payment.getAmount(),
                        "status", order.getStatus().name());

        realtimeEventPublisher.publishAfterCommit(
                "/topic/cashier",
                "PAYMENT_RECORDED",
                payment.getId().toString(),
                sessionId,
                "1",
                eventData);

        if (order.getSession() != null) {
            realtimeEventPublisher.publishAfterCommit(
                    "/topic/table-sessions/"
                            + order.getSession().getId(),
                    "PAYMENT_RECORDED",
                    payment.getId().toString(),
                    sessionId,
                    "1",
                    eventData);
        }

        return toPaymentResponse(payment);
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
    
    private PaymentDto.PaymentResponse toPaymentResponse(
            Payment payment) {

        return PaymentDto.PaymentResponse.builder()
                .paymentId(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .reference(payment.getReference())
                .recordedByEmail(
                        payment.getRecordedBy() != null
                                ? payment.getRecordedBy().getEmail()
                                : null)
                .paidAt(payment.getPaidAt())
                .build();
    }

}
