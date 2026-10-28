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
import vn.edu.ute.milktea.security.CurrentActor;

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
    public vn.edu.ute.milktea.dto.CancellationDto.RefundResponse requestBankRefund(Long orderId, Long cashierId) {
        return requestBankRefund(orderId, cashierId != null ? CurrentActor.authenticated(cashierId, vn.edu.ute.milktea.entity.account.Role.CASHIER, null, java.util.List.of()) : null);
    }

    @Transactional
    public vn.edu.ute.milktea.dto.CancellationDto.RefundResponse refundCash(Long orderId, Long cashierId) {
        return refundCash(orderId, cashierId != null ? CurrentActor.authenticated(cashierId, vn.edu.ute.milktea.entity.account.Role.CASHIER, null, java.util.List.of()) : null);
    }

    @Transactional
    public vn.edu.ute.milktea.dto.CancellationDto.RefundResponse requestBankRefund(Long orderId, CurrentActor actor) {
        var order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (order.getStatus() != vn.edu.ute.milktea.entity.order.OrderStatus.DA_HUY) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Chỉ có thể hoàn tiền cho đơn đã hủy");
        }
        if (actor != null && !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            if (order.getBranch() != null && !actor.canAccessBranch(order.getBranch().getId())) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền hoàn tiền cho đơn ngoài chi nhánh");
            }
        }
        var existing = refundRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            var ex = existing.get();
            if (ex.getStatus() != RefundStatus.TU_CHOI) {
                return toResponse(ex);
            }
            // F16: Khi bị TU_CHOI, cập nhật lại bản ghi cũ sang CHO_DUYET thay vì insert mới vi phạm UQ_YeuCauHoanTien_MaDonHang
            ex.setStatus(RefundStatus.CHO_DUYET);
            ex.setReason("Gửi lại yêu cầu hoàn tiền chuyển khoản cho đơn #" + orderId);
            ex.setApprovedBy(null);
            ex.setApprovedAt(null);
            ex.setApprovalReason(null);
            ex.setRefundedBy(null);
            ex.setRefundedAt(null);
            ex.setRefundReference(null);
            ex = refundRepository.save(ex);
            auditRepository.save(BusinessAudit.builder()
                    .orderId(order.getId())
                    .accountId(actor != null ? actor.getAccountId() : null)
                    .action("BANK_REFUND_RESUBMITTED")
                    .reason("Gửi lại yêu cầu hoàn tiền chuyển khoản sau khi bị từ chối")
                    .createdAt(Instant.now())
                    .build());
            publish(ex);
            return toResponse(ex);
        }
        var invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy hóa đơn"));
        var payment = paymentRepository.findByInvoiceId(invoice.getId())
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Đơn chưa thu tiền, không cần hoàn tiền"));
        if (payment.getMethod() != vn.edu.ute.milktea.entity.payment.PaymentMethod.BANK_TRANSFER) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Đơn thanh toán tiền mặt phải do thu ngân hoàn trực tiếp");
        }
        var refund = refundRepository.save(RefundRequest.builder()
                .order(order)
                .payment(payment)
                .amount(payment.getAmount())
                .reason("Thu ngân gửi yêu cầu hoàn tiền chuyển khoản cho đơn #" + orderId)
                .status(RefundStatus.CHO_DUYET)
                .idempotencyKey("REFUND_BANK_" + orderId)
                .build());
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(actor != null ? actor.getAccountId() : null)
                .action("BANK_REFUND_REQUESTED")
                .reason("Gửi yêu cầu hoàn tiền chuyển khoản")
                .createdAt(Instant.now())
                .build());
        publish(refund);
        return toResponse(refund);
    }

    @Transactional
    public vn.edu.ute.milktea.dto.CancellationDto.RefundResponse refundCash(Long orderId, CurrentActor actor) {
        var order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy đơn hàng"));
        if (order.getStatus() != vn.edu.ute.milktea.entity.order.OrderStatus.DA_HUY) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Chỉ có thể hoàn tiền cho đơn đã hủy");
        }
        if (actor != null && !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            if (order.getBranch() != null && !actor.canAccessBranch(order.getBranch().getId())) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Không có quyền hoàn tiền cho đơn ngoài chi nhánh");
            }
        }
        Long cashierId = actor != null ? actor.getAccountId() : null;
        var existing = refundRepository.findByOrderId(order.getId());
        if (existing.isPresent() && existing.get().getStatus() == RefundStatus.DA_HOAN) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Đơn hàng đã được hoàn tiền trước đó");
        }
        var invoice = invoiceRepository.findByOrderId(order.getId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ORDER_STATE_CONFLICT, "Không tìm thấy hóa đơn"));
        var payment = paymentRepository.findByInvoiceId(invoice.getId())
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Đơn chưa thu tiền, không cần hoàn tiền"));
        if (payment.getMethod() != vn.edu.ute.milktea.entity.payment.PaymentMethod.CASH) {
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Đơn thanh toán chuyển khoản phải gửi yêu cầu hoàn tiền đến Admin");
        }
        Account cashier = cashierId != null ? accountRepository.findById(cashierId).orElse(null) : null;
        RefundRequest refund;
        if (existing.isPresent()) {
            refund = existing.get();
            refund.setStatus(RefundStatus.DA_HOAN);
            refund.setRefundedBy(cashier);
            refund.setRefundedAt(Instant.now());
            refund.setRefundReference("CASH_REFUND_" + orderId);
        } else {
            refund = RefundRequest.builder()
                    .order(order)
                    .payment(payment)
                    .amount(payment.getAmount())
                    .reason("Thu ngân thực trả và ghi nhận hoàn tiền mặt")
                    .status(RefundStatus.DA_HOAN)
                    .refundedBy(cashier)
                    .refundedAt(Instant.now())
                    .refundReference("CASH_REFUND_" + orderId)
                    .idempotencyKey("REFUND_CASH_" + orderId)
                    .build();
        }
        refund = refundRepository.save(refund);
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(cashierId)
                .action("CASH_REFUNDED")
                .reason("Hoàn tiền mặt thực tế: " + refund.getAmount() + " VND")
                .createdAt(Instant.now())
                .build());
        publish(refund);
        return toResponse(refund);
    }

    @Transactional
    public vn.edu.ute.milktea.dto.CancellationDto.RefundResponse requestFromCancellation(Long cancellationId, CurrentActor actor) {
        var cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy phiếu hủy"));
        var order = orderRepository.findByIdWithLock(cancellation.getOrder().getId()).orElseThrow();
        if (order.getStatus() != vn.edu.ute.milktea.entity.order.OrderStatus.DA_HUY || cancellation.getStatus() != vn.edu.ute.milktea.entity.cancellation.CancellationStatus.CHAP_THUAN)
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Phiếu hủy chưa được chấp thuận");
        var existing = refundRepository.findByOrderId(order.getId());
        if (existing.isPresent() && existing.get().getStatus() != RefundStatus.TU_CHOI) {
            return toResponse(existing.get());
        }
        var invoice = invoiceRepository.findByOrderId(order.getId()).orElseThrow();
        var payment = paymentRepository.findByInvoiceId(invoice.getId())
                .orElseThrow(() -> BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Đơn chưa thu tiền, không cần hoàn tiền"));
        
        if (payment.getMethod() == vn.edu.ute.milktea.entity.payment.PaymentMethod.CASH) {
            return refundCash(order.getId(), actor);
        }
        return requestBankRefund(order.getId(), actor);
    }

    @Transactional
    public void decideRefund(Long refundId, boolean approved, String reason, String reference, Long adminId) {
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
            // F16: Nếu có mã tham chiếu giao dịch thì hoàn tất ngay sang DA_HOAN, nếu chưa thì chuyển DA_DUYET để thủ quỹ/thu ngân thực chi
            if (reference != null && !reference.isBlank()) {
                refund.setStatus(RefundStatus.DA_HOAN);
                refund.setRefundedBy(admin);
                refund.setRefundedAt(refund.getApprovedAt());
                refund.setRefundReference(reference);
                auditRepository.save(BusinessAudit.builder()
                        .orderId(refund.getOrder().getId())
                        .accountId(adminId)
                        .action("BANK_REFUND_EXECUTED")
                        .reason("Admin duyệt và xác nhận đã hoàn tất giao dịch ngân hàng: " + reference)
                        .createdAt(Instant.now())
                        .build());
            } else {
                refund.setStatus(RefundStatus.DA_DUYET);
                auditRepository.save(BusinessAudit.builder()
                        .orderId(refund.getOrder().getId())
                        .accountId(adminId)
                        .action("BANK_REFUND_APPROVED")
                        .reason(reason != null && !reason.isBlank() ? reason : "Admin phê duyệt yêu cầu hoàn tiền")
                        .createdAt(Instant.now())
                        .build());
            }
        } else {
            refund.setStatus(RefundStatus.TU_CHOI);
            auditRepository.save(BusinessAudit.builder()
                    .orderId(refund.getOrder().getId())
                    .accountId(adminId)
                    .action("BANK_REFUND_REJECTED")
                    .reason(reason)
                    .createdAt(Instant.now())
                    .build());
        }

        refundRepository.save(refund);
        publish(refund);
    }

    @Transactional
    public void decideRefund(Long refundId, boolean approved, String reason, Long adminId) {
        decideRefund(refundId, approved, reason, null, adminId);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository;

    public void setStaffBranchRepository(vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository) {
        this.staffBranchRepository = staffBranchRepository;
    }

    @Transactional
    public void recordRefundPayment(Long refundId, String reference, Long cashierId) {
        if (reference == null || reference.trim().isEmpty()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED, "Mã tham chiếu thực chi hoàn tiền là bắt buộc");
        }

        RefundRequest refund = refundRepository.findByIdWithLock(refundId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getStatus() != RefundStatus.DA_DUYET) {
            throw BusinessException.conflict(ErrorCode.REFUND_NOT_APPROVED,
                    "Chỉ có thể thực chi hoàn tiền cho các yêu cầu đã được Admin phê duyệt (DA_DUYET)");
        }

        if (cashierId == null) {
            throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Chưa xác định nhân viên thực chi hoàn tiền");
        }

        Account cashier = accountRepository.findById(cashierId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy nhân viên thu ngân"));

        Long orderBranchId = refund.getOrder().getBranch() != null ? refund.getOrder().getBranch().getId() : null;
        if (cashier.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN) {
            if (orderBranchId == null) {
                throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Đơn hàng thiếu thông tin chi nhánh");
            }
            boolean hasActiveAssignment = staffBranchRepository != null && staffBranchRepository
                    .findByAccountIdAndBranchIdAndStatus(cashier.getId(), orderBranchId, "HOAT_DONG")
                    .isPresent();
            if (!hasActiveAssignment && cashier.getBranch() != null && cashier.getBranch().getId().equals(orderBranchId)) {
                hasActiveAssignment = true;
            }
            if (!hasActiveAssignment) {
                throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Thu ngân không thuộc chi nhánh của đơn hàng");
            }
        }

        refund.setStatus(RefundStatus.DA_HOAN);
        refund.setRefundedBy(cashier);
        refund.setRefundedAt(Instant.now());
        refund.setRefundReference(reference.trim());
        refundRepository.save(refund);
        publish(refund);

        auditRepository.save(BusinessAudit.builder()
                .orderId(refund.getOrder().getId())
                .accountId(cashierId)
                .action("REFUND_EXECUTED")
                .reason("Thực chi hoàn: " + refund.getAmount() + " VND, tham chiếu: " + reference.trim())
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public java.util.List<vn.edu.ute.milktea.dto.CancellationDto.RefundResponse> getRefunds(RefundStatus status) {
        return getRefunds(status, null);
    }

    @Transactional(readOnly = true)
    public java.util.List<vn.edu.ute.milktea.dto.CancellationDto.RefundResponse> getRefunds(RefundStatus status, Long branchId) {
        java.util.List<RefundRequest> list;
        if (branchId != null) {
            list = status != null ?
                    refundRepository.findByStatusAndBranchId(status, branchId) :
                    refundRepository.findByBranchId(branchId);
        } else {
            list = status != null ?
                    refundRepository.findByStatus(status) :
                    refundRepository.findAll();
        }
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
        if (order.getBranch() != null) {
            publisher.publishAfterCommit("/topic/branches/" + order.getBranch().getId() + "/cashier", "REFUND_CHANGED", r.getId().toString(), sid, "1", data);
        }
        if (sid != null) publisher.publishAfterCommit("/topic/table-sessions/" + sid, "REFUND_CHANGED", r.getId().toString(), sid, "1", data);
    }
}
