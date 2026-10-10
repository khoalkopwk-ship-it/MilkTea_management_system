package vn.edu.ute.milktea.service.cancellation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.cancellation.RefundRequest;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.repository.order.OrderRepository orderRepository;
    private final vn.edu.ute.milktea.repository.order.InvoiceRepository invoiceRepository;
    private final vn.edu.ute.milktea.repository.payment.PaymentRepository paymentRepository;
    private final vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository cancellationRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher publisher;

    @Transactional
    public vn.edu.ute.milktea.dto.CancellationDto.RefundResponse requestFromCancellation(Long cancellationId, Long staffId) {
        var cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy phiếu hủy"));
        var order = orderRepository.findByIdWithLock(cancellation.getOrder().getId()).orElseThrow();
        if (order.getStatus() != vn.edu.ute.milktea.entity.order.OrderStatus.DA_HUY || cancellation.getStatus() != vn.edu.ute.milktea.entity.cancellation.CancellationStatus.CHAP_THUAN)
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Phiếu hủy chưa được chấp thuận");
        var existing = refundRepository.findByOrderId(order.getId());
        if (existing.isPresent()) return toResponse(existing.get());
        var invoice = invoiceRepository.findByOrderId(order.getId()).orElseThrow();
        var payment = paymentRepository.findByInvoiceId(invoice.getId())
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Đơn chưa thu tiền, không cần hoàn tiền"));
        var refund = refundRepository.save(RefundRequest.builder().order(order).payment(payment)
                .amount(payment.getAmount()).reason(cancellation.getReason())
                .status(RefundStatus.CHO_DUYET).idempotencyKey("CANCEL_" + cancellationId).build());
        auditRepository.save(BusinessAudit.builder().orderId(order.getId()).accountId(staffId)
                .action("REFUND_REQUESTED").reason(cancellation.getReason()).createdAt(Instant.now()).build());
        publish(refund);
        return toResponse(refund);
    }

    @Transactional
    public void decideRefund(Long refundId, boolean approved, String reason, Long adminId) {
        RefundRequest refund = refundRepository.findByIdWithLock(refundId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getStatus() != RefundStatus.CHO_DUYET) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Yêu cầu hoàn tiền đã được xử lý trước đó");
        }

        Account admin = adminId != null ? accountRepository.findById(adminId).orElse(null) : null;
        refund.setApprovedBy(admin);
        refund.setApprovedAt(Instant.now());
        refund.setApprovalReason(reason);

        if (approved) {
            refund.setStatus(RefundStatus.DA_HOAN);
            refund.setRefundedBy(admin);
            refund.setRefundedAt(refund.getApprovedAt());
            refund.setRefundReference("ADMIN_APPROVAL_" + refundId);
            auditRepository.save(BusinessAudit.builder()
                    .orderId(refund.getOrder().getId())
                    .accountId(adminId)
                    .action("REFUND_APPROVED")
                    .reason(reason)
                    .createdAt(Instant.now())
                    .build());
        } else {
            refund.setStatus(RefundStatus.TU_CHOI);
            auditRepository.save(BusinessAudit.builder()
                    .orderId(refund.getOrder().getId())
                    .accountId(adminId)
                    .action("REFUND_REJECTED")
                    .reason(reason)
                    .createdAt(Instant.now())
                    .build());
        }

        refundRepository.save(refund);
        publish(refund);
    }

    @Transactional
    public void recordRefundPayment(Long refundId, String reference, Long cashierId) {
        RefundRequest refund = refundRepository.findByIdWithLock(refundId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getStatus() != RefundStatus.DA_DUYET) {
            throw BusinessException.conflict(ErrorCode.REFUND_NOT_APPROVED,
                    "Chỉ có thể thực chi hoàn tiền cho các yêu cầu đã được Admin phê duyệt (DA_DUYET)");
        }

        Account cashier = cashierId != null ? accountRepository.findById(cashierId).orElse(null) : null;
        refund.setStatus(RefundStatus.DA_HOAN);
        refund.setRefundedBy(cashier);
        refund.setRefundedAt(Instant.now());
        refund.setRefundReference(reference);
        refundRepository.save(refund);
        publish(refund);

        auditRepository.save(BusinessAudit.builder()
                .orderId(refund.getOrder().getId())
                .accountId(cashierId)
                .action("REFUND_EXECUTED")
                .reason("Thực chi hoàn: " + refund.getAmount() + " VND, tham chiếu: " + reference)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public java.util.List<vn.edu.ute.milktea.dto.CancellationDto.RefundResponse> getRefunds(RefundStatus status) {
        java.util.List<RefundRequest> list = status != null ?
                refundRepository.findByStatus(status) :
                refundRepository.findAll();
        return list.stream().map(RefundService::toResponse).toList();
    }

    public static vn.edu.ute.milktea.dto.CancellationDto.RefundResponse toResponse(RefundRequest r) {
        return vn.edu.ute.milktea.dto.CancellationDto.RefundResponse.builder()
                .id(r.getId()).orderId(r.getOrder().getId()).paymentId(r.getPayment().getId())
                .amount(r.getAmount()).reason(r.getReason()).status(r.getStatus())
                .approvedBy(r.getApprovedBy() != null ? r.getApprovedBy().getFullName() : null)
                .approvedAt(r.getApprovedAt()).refundedAt(r.getRefundedAt()).build();
    }

    private void publish(RefundRequest r) {
        var order = r.getOrder();
        String sid = order.getSession() != null ? order.getSession().getId().toString() : null;
        var data = java.util.Map.<String, Object>of("orderId", order.getId(), "refundId", r.getId(), "status", r.getStatus().name());
        publisher.publishAfterCommit("/topic/admin", "REFUND_CHANGED", r.getId().toString(), sid, "1", data);
        publisher.publishAfterCommit("/topic/cashier", "REFUND_CHANGED", r.getId().toString(), sid, "1", data);
        if (sid != null) publisher.publishAfterCommit("/topic/table-sessions/" + sid, "REFUND_CHANGED", r.getId().toString(), sid, "1", data);
    }
}
