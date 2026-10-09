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

    @Transactional
    public void decideRefund(Long refundId, boolean approved, String reason, Long adminId) {
        RefundRequest refund = refundRepository.findById(refundId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getStatus() != RefundStatus.CHO_DUYET) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Yêu cầu hoàn tiền đã được xử lý trước đó");
        }

        Account admin = adminId != null ? accountRepository.findById(adminId).orElse(null) : null;
        refund.setApprovedBy(admin);
        refund.setApprovedAt(Instant.now());
        refund.setApprovalReason(reason);

        if (approved) {
            refund.setStatus(RefundStatus.DA_DUYET);
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
    }

    @Transactional
    public void recordRefundPayment(Long refundId, String reference, Long cashierId) {
        RefundRequest refund = refundRepository.findById(refundId)
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
        return list.stream().map(r -> vn.edu.ute.milktea.dto.CancellationDto.RefundResponse.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .paymentId(r.getPayment().getId())
                .amount(r.getAmount())
                .reason(r.getReason())
                .status(r.getStatus())
                .approvedAt(r.getApprovedAt())
                .refundedAt(r.getRefundedAt())
                .build()
        ).toList();
    }
}
