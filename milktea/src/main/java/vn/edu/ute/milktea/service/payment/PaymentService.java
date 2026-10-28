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
    private final vn.edu.ute.milktea.repository.table.DiningTableRepository tableRepository;
    private final vn.edu.ute.milktea.service.table.TableSessionService tableSessionService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentNoticeRepository paymentNoticeRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    private final vn.edu.ute.milktea.repository.branch.StaffBranchRepository staffBranchRepository;

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

        // Lock table before order/invoice to match table order creation and settlement.
        if (invoiceLookup.getOrder().getSession() != null) {
            tableRepository.findByIdWithLock(invoiceLookup.getOrder().getSession().getTable().getId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));
        }
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

        // R01/R10: Xác định và kiểm tra quyền thu ngân TRƯỚC khi xử lý replay idempotency
        if (cashierId == null) {
            throw BusinessException.conflict(
                    ErrorCode.ACCESS_DENIED,
                    "Chưa xác định nhân viên thu tiền");
        }

        Account cashier = accountRepository.findById(cashierId)
                .orElseThrow(() -> BusinessException.notFound(
                        ErrorCode.VALIDATION_FAILED,
                        "Không tìm thấy nhân viên thu tiền"));

        // F06/R01: Kiểm tra thu ngân thuộc chi nhánh của đơn hàng (dựa trên StaffBranch active hoặc Account.branch)
        if (cashier.getRole() != vn.edu.ute.milktea.entity.account.Role.ADMIN) {
            Long orderBranchId = order.getBranch() != null ? order.getBranch().getId() : null;
            if (orderBranchId == null) {
                throw BusinessException.conflict(
                        ErrorCode.VALIDATION_FAILED,
                        "Đơn hàng thiếu thông tin chi nhánh");
            }
            boolean hasActiveAssignment = staffBranchRepository != null && staffBranchRepository
                    .findByAccountIdAndBranchIdAndStatus(cashier.getId(), orderBranchId, "HOAT_DONG")
                    .isPresent();
            if (!hasActiveAssignment && cashier.getBranch() != null && cashier.getBranch().getId().equals(orderBranchId)) {
                hasActiveAssignment = true;
            }
            if (!hasActiveAssignment) {
                throw BusinessException.forbidden(
                        ErrorCode.ACCESS_DENIED,
                        "Thu ngân không thuộc chi nhánh của đơn hàng");
            }
        }

        // 4. Xử lý request gửi lại với cùng idempotency key (sau khi đã kiểm tra quyền actor - R10)
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

        if (order.getStatus() == OrderStatus.DA_HUY) {
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Không thể thu tiền đơn đã hủy");
        }
        OrderStatus previousStatus = order.getStatus();

        // 8. Không ghi khoản thu thứ hai cho cùng hóa đơn
        if (paymentRepository.existsByInvoiceId(invoiceId)) {
            throw BusinessException.conflict(
                    ErrorCode.PAYMENT_ALREADY_RECORDED,
                    "Hóa đơn đã được thanh toán");
        }

        // 9. Kiểm tra số tiền hóa đơn (cho phép 0 VND khi giảm giá 100% bằng voucher - F17)
        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().signum() < 0) {
            throw BusinessException.conflict(
                    ErrorCode.VALIDATION_FAILED,
                    "Số tiền phải trả không hợp lệ");
        }

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
        if (order.getStatus() == OrderStatus.CHO_THANH_TOAN) {
            order.setStatus(isStaffCreated(order) ? OrderStatus.CHO_CHE_BIEN : OrderStatus.CHO_XAC_NHAN);
        }
        orderRepository.save(order);

        // 13. Ghi lịch sử nghiệp vụ
        auditRepository.save(BusinessAudit.builder()
                .orderId(order.getId())
                .accountId(cashierId)
                .action("PAYMENT_RECORDED")
                .beforeState(previousStatus.name())
                .afterState(order.getStatus().name())
                .reason("Ghi thu " + request.getMethod()
                        + ": " + payment.getAmount() + " VND")
                .createdAt(Instant.now())
                .build());

        // 14. Phát sự kiện sau khi transaction commit (R09: phát cả kênh branch và kênh global)
        String sessionId = order.getSession() != null
                ? order.getSession().getId().toString()
                : null;
        Long branchId = order.getBranch() != null ? order.getBranch().getId() : null;

        java.util.Map<String, Object> eventData =
                java.util.Map.of(
                        "orderId", order.getId(),
                        "invoiceId", invoice.getId(),
                        "amount", payment.getAmount(),
                        "status", order.getStatus().name(),
                        "branchId", branchId != null ? branchId : 0L);

        realtimeEventPublisher.publishAfterCommit(
                "/topic/cashier",
                "PAYMENT_RECORDED",
                payment.getId().toString(),
                sessionId,
                "1",
                eventData);

        if (branchId != null) {
            realtimeEventPublisher.publishAfterCommit(
                    "/topic/branches/" + branchId + "/cashier",
                    "PAYMENT_RECORDED",
                    payment.getId().toString(),
                    sessionId,
                    "1",
                    eventData);
        }

        if (order.getStatus() == OrderStatus.CHO_CHE_BIEN) {
            realtimeEventPublisher.publishAfterCommit("/topic/kitchen", "ORDER_STATUS_CHANGED",
                    order.getId().toString(), sessionId, "1", eventData);
            if (branchId != null) {
                realtimeEventPublisher.publishAfterCommit("/topic/branches/" + branchId + "/kitchen", "ORDER_STATUS_CHANGED",
                        order.getId().toString(), sessionId, "1", eventData);
            }
        }

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
    public void createAuthorizedNotice(Long invoiceId, PaymentDto.PaymentNoticeRequest request, String key, vn.edu.ute.milktea.security.CurrentActor actor) {
        var invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn"));
        var order = invoice.getOrder();
        boolean allowed = actor != null && (actor.hasRole(vn.edu.ute.milktea.entity.account.Role.CASHIER) || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN) || actor.hasRole(vn.edu.ute.milktea.entity.account.Role.MANAGER)
                || (actor.getAccountId() != null && order.getAccount() != null && actor.getAccountId().equals(order.getAccount().getId()))
                || (actor.getSessionId() != null && order.getSession() != null && actor.getSessionId().equals(order.getSession().getId()))
                || (actor.getOrderId() != null && actor.getOrderId().equals(order.getId())));
        if (!allowed) throw BusinessException.forbidden(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thanh toán hóa đơn này");
        createNotice(invoiceId, request, key);
    }

    @Transactional
    public void createNotice(Long invoiceId, PaymentDto.PaymentNoticeRequest request, String idempotencyKey) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn"));

        if (request == null || request.getMethod() == null)
            throw BusinessException.conflict(ErrorCode.VALIDATION_FAILED, "Chọn phương thức tiền mặt hoặc chuyển khoản");
        if (invoice.getOrder().getStatus() == OrderStatus.DA_HUY || invoice.getStatus() == InvoiceStatus.DA_HUY)
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Không thanh toán đơn đã hủy");
        if (paymentRepository.existsByInvoiceId(invoiceId))
            throw BusinessException.conflict(ErrorCode.ORDER_STATE_CONFLICT, "Đơn đã được ghi thu tiền");
        var latest = paymentNoticeRepository.findFirstByInvoiceIdOrderByCreatedAtDescIdDesc(invoiceId);
        if (latest.isPresent() && latest.get().getMethod() == request.getMethod()
                && java.util.Objects.equals(latest.get().getReference(), request.getReference())) return;
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
                .reason("Khách yêu cầu thanh toán " + request.getMethod() + ": " + request.getReference())
                .createdAt(Instant.now())
                .build());

        var noticeData = java.util.Map.of(
                "orderId", invoice.getOrder().getId(),
                "invoiceId", invoice.getId(),
                "method", request.getMethod(),
                "tableName", invoice.getOrder().getSession() != null ? invoice.getOrder().getSession().getTable().getName() : "Quầy"
        );
        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "PAYMENT_NOTICE_CREATED", notice.getId().toString(), null, "1", noticeData);
        if (invoice.getOrder().getBranch() != null) {
            realtimeEventPublisher.publishAfterCommit("/topic/branches/" + invoice.getOrder().getBranch().getId() + "/cashier", "PAYMENT_NOTICE_CREATED", notice.getId().toString(), null, "1", noticeData);
        }
    }

    @Transactional
    public PaymentDto.PaymentResponse confirmTransferReceiptForOrder(Long orderId, Long cashierId, String idempotencyKey) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_FAILED, "Không tìm thấy hóa đơn cho đơn hàng #" + orderId));
        if (paymentRepository.existsByInvoiceId(invoice.getId())) {
            Payment existing = paymentRepository.findByInvoiceId(invoice.getId()).orElse(null);
            if (existing != null) {
                return toPaymentResponse(existing);
            }
        }
        PaymentDto.RecordPaymentRequest request = PaymentDto.RecordPaymentRequest.builder()
                .method(PaymentMethod.BANK_TRANSFER)
                .reference("TRANSFER_CONFIRMED_" + orderId)
                .build();
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "TRANSFER_CONFIRM_" + orderId;
        return recordReceipt(invoice.getId(), request, cashierId, effectiveKey);
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
    
    private boolean isStaffCreated(Order order) {
        return order.getAccount() != null && (order.getAccount().getRole() == vn.edu.ute.milktea.entity.account.Role.CASHIER
                || order.getAccount().getRole() == vn.edu.ute.milktea.entity.account.Role.ADMIN
                || order.getAccount().getRole() == vn.edu.ute.milktea.entity.account.Role.MANAGER);
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
