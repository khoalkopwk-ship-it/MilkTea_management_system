package vn.edu.ute.milktea.service.cancellation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.dto.cancellation.CancellationDto;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.cancellation.CancellationRequest;
import vn.edu.ute.milktea.entity.cancellation.CancellationStatus;
import vn.edu.ute.milktea.entity.cancellation.RefundRequest;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.order.Order;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.Payment;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.cancellation.CancellationRequestRepository;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.security.CurrentActor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancellationService {

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CancellationRequestRepository cancellationRepository;
    private final RefundRequestRepository refundRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;

    @Transactional
    public CancellationDto.CancellationResponse requestCancellation(
            Long orderId, CancellationDto.CreateCancellationRequest request, CurrentActor actor) {

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));

        // Chỉ được yêu cầu hủy trước khi bắt đầu pha chế
        if (order.getStatus() != OrderStatus.CHO_XAC_NHAN && order.getStatus() != OrderStatus.CHO_CHE_BIEN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                    "Không thể yêu cầu hủy đơn đã hoặc đang được pha chế: " + order.getStatus());
        }

        // Kiểm tra không có yêu cầu hủy nào đang chờ duyệt
        var existing = cancellationRepository.findFirstByOrderIdAndStatus(orderId, CancellationStatus.CHO);
        if (existing.isPresent()) {
            throw BusinessException.conflict(ErrorCode.CANCELLATION_PENDING, "Đơn hàng đã có yêu cầu hủy đang chờ xử lý");
        }

        CancellationRequest cancelReq = CancellationRequest.builder()
                .order(order)
                .reason(request.getReason())
                .status(CancellationStatus.CHO)
                .createdAt(Instant.now())
                .build();
        cancelReq = cancellationRepository.save(cancelReq);

        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .action("CANCELLATION_REQUESTED")
                .reason(request.getReason())
                .createdAt(Instant.now())
                .build());

        return CancellationDto.CancellationResponse.builder()
                .id(cancelReq.getId())
                .orderId(order.getId())
                .reason(cancelReq.getReason())
                .status(cancelReq.getStatus())
                .createdAt(cancelReq.getCreatedAt())
                .build();
    }

    @Transactional
    public void decideCancellation(Long requestId, CancellationDto.DecideCancellationRequest request, Long cashierId) {
        CancellationRequest cancelReq = cancellationRepository.findById(requestId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy yêu cầu hủy"));

        if (cancelReq.getStatus() != CancellationStatus.CHO) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Yêu cầu hủy đã được xử lý trước đó");
        }

        Order order = orderRepository.findByIdWithLock(cancelReq.getOrder().getId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));

        Account cashier = cashierId != null ? accountRepository.findById(cashierId).orElse(null) : null;
        cancelReq.setDecidedBy(cashier);
        cancelReq.setDecidedAt(Instant.now());
        cancelReq.setDecisionReason(request.getReason());

        if (Boolean.TRUE.equals(request.getApproved())) {
            // Chấp thuận hủy
            if (order.getStatus() == OrderStatus.DANG_CHE_BIEN || order.getStatus() == OrderStatus.HOAN_THANH) {
                throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT,
                        "Không thể chấp thuận hủy vì đơn đã bắt đầu hoặc hoàn thành pha chế: " + order.getStatus());
            }

            cancelReq.setStatus(CancellationStatus.CHAP_THUAN);
            order.setStatus(OrderStatus.DA_HUY);
            orderRepository.save(order);

            // Cập nhật hóa đơn sang DA_HUY
            Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElse(null);
            if (invoice != null) {
                invoice.setStatus(InvoiceStatus.DA_HUY);
                invoice.setCancelledAt(Instant.now());
                invoiceRepository.save(invoice);

                // Nếu đã thu tiền, tạo RefundRequest chờ Admin duyệt
                Optional<Payment> paymentOpt = paymentRepository.findByInvoiceId(invoice.getId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    RefundRequest refund = RefundRequest.builder()
                            .order(order)
                            .payment(payment)
                            .amount(payment.getAmount())
                            .reason("Hoàn tiền do hủy đơn #" + order.getId() + ": " + cancelReq.getReason())
                            .status(RefundStatus.CHO_DUYET)
                            .idempotencyKey(UUID.randomUUID().toString())
                            .build();
                    refundRepository.save(refund);
                }
            }

            auditRepository.save(BusinessAudit.builder()
                    .orderId(order.getId())
                    .accountId(cashierId)
                    .action("CANCELLATION_APPROVED")
                    .reason(request.getReason())
                    .createdAt(Instant.now())
                    .build());
        } else {
            // Từ chối hủy
            cancelReq.setStatus(CancellationStatus.TU_CHOI);
            auditRepository.save(BusinessAudit.builder()
                    .orderId(order.getId())
                    .accountId(cashierId)
                    .action("CANCELLATION_REJECTED")
                    .reason(request.getReason())
                    .createdAt(Instant.now())
                    .build());
        }

        cancellationRepository.save(cancelReq);
    }

    @Transactional(readOnly = true)
    public java.util.List<CancellationDto.CancellationResponse> getCancellationRequests(CancellationStatus status) {
        java.util.List<CancellationRequest> list = status != null ?
                cancellationRepository.findByStatus(status) :
                cancellationRepository.findAll();
        return list.stream().map(c -> CancellationDto.CancellationResponse.builder()
                .id(c.getId())
                .orderId(c.getOrder().getId())
                .reason(c.getReason())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .decidedAt(c.getDecidedAt())
                .build()
        ).toList();
    }
}
