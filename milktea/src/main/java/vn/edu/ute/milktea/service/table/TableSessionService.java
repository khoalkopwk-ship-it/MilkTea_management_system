package vn.edu.ute.milktea.service.table;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.entity.account.Account;
import vn.edu.ute.milktea.entity.audit.BusinessAudit;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.table.*;
import vn.edu.ute.milktea.repository.account.AccountRepository;
import vn.edu.ute.milktea.repository.audit.BusinessAuditRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;
import vn.edu.ute.milktea.repository.table.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableSessionService {

    private final DiningTableRepository tableRepository;
    private final TableSessionRepository sessionRepository;
    private final SessionCartRepository cartRepository;
    private final SessionCartItemRepository cartItemRepository;
    private final TableSessionAccessRepository accessRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final BusinessAuditRepository auditRepository;
    private final vn.edu.ute.milktea.service.realtime.RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public TableSession openByCashier(Long tableId, Long cashierId) {
        DiningTable table = tableRepository.findByIdWithLock(tableId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));

        if (table.getActiveSessionId() != null) {
            var existingOpt = sessionRepository.findById(table.getActiveSessionId());
            if (existingOpt.isPresent() && existingOpt.get().getStatus() == TableSessionStatus.OPEN) {
                return existingOpt.get();
            }
        }

        Account cashier = cashierId != null ? accountRepository.findById(cashierId).orElse(null) : null;

        TableSession session = TableSession.builder()
                .table(table)
                .status(TableSessionStatus.OPEN)
                .openedAt(Instant.now())
                .openedBy(cashier)
                .version(0L)
                .build();
        session = sessionRepository.save(session);

        table.setStatus(TableStatus.CO_KHACH);
        table.setActiveSessionId(session.getId());
        tableRepository.save(table);

        SessionCart cart = SessionCart.builder()
                .session(session)
                .version(1L)
                .modifiedAt(Instant.now())
                .build();
        cartRepository.save(cart);

        auditRepository.save(BusinessAudit.builder()
                .sessionId(session.getId())
                .accountId(cashierId)
                .action("TABLE_SESSION_OPENED_BY_CASHIER")
                .afterState("OPEN")
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "TABLE_SESSION_OPENED", session.getId().toString(), session.getId().toString(), "1", java.util.Map.of("tableId", table.getId(), "tableName", table.getName()));
        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + session.getId(), "TABLE_SESSION_OPENED", session.getId().toString(), session.getId().toString(), "1", java.util.Map.of("tableId", table.getId(), "tableName", table.getName()));

        return session;
    }

    @Transactional
    public void closeByCashier(Long tableId, Long cashierId) {
        DiningTable table = tableRepository.findByIdWithLock(tableId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_NOT_FOUND, "Không tìm thấy bàn"));

        if (table.getActiveSessionId() == null) {
            throw BusinessException.badRequest(ErrorCode.TABLE_SESSION_CLOSED, "Bàn chưa có phiên mở");
        }

        TableSession session = sessionRepository.findByIdWithLock(table.getActiveSessionId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_SESSION_CLOSED, "Không tìm thấy phiên"));

        if (session.getStatus() != TableSessionStatus.OPEN) {
            throw BusinessException.badRequest(ErrorCode.TABLE_SESSION_CLOSED, "Phiên bàn đã kết thúc trước đó");
        }

        // 1. Kiểm tra mọi đơn hàng thuộc phiên đều đã hoàn thành hoặc đã hủy
        long unfinishedCount = orderRepository.countUnfinishedOrdersBySessionId(session.getId());
        if (unfinishedCount > 0) {
            throw BusinessException.conflict(ErrorCode.TABLE_CLOSE_NOT_ALLOWED,
                    "Không thể đóng phiên: Còn " + unfinishedCount + " đơn hàng chưa hoàn thành hoặc hủy");
        }

        // 2. Kiểm tra mọi hóa đơn HIEU_LUC đã được thu đủ
        List<Invoice> invoices = invoiceRepository.findBySessionId(session.getId());
        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.HIEU_LUC) {
                // Kiểm tra đã thanh toán chưa
                boolean hasPayment = paymentRepository.existsByInvoiceId(invoice.getId());
                if (!hasPayment && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    throw BusinessException.conflict(ErrorCode.TABLE_CLOSE_NOT_ALLOWED,
                            "Không thể đóng phiên: Hóa đơn #" + invoice.getId() + " chưa được thanh toán đủ");
                }
            }
        }

        Account cashier = cashierId != null ? accountRepository.findById(cashierId).orElse(null) : null;

        // 3. Đóng phiên & chuyển bàn về TRONG
        session.setStatus(TableSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        session.setClosedBy(cashier);
        sessionRepository.save(session);

        table.setStatus(TableStatus.TRONG);
        table.setActiveSessionId(null);
        tableRepository.save(table);

        // 4. Xóa giỏ phiên tạm
        cartRepository.findBySessionId(session.getId()).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            cartRepository.delete(cart);
        });

        // 5. Thu hồi quyền truy cập phiên của khách
        accessRepository.deleteBySessionId(session.getId());

        auditRepository.save(BusinessAudit.builder()
                .sessionId(session.getId())
                .accountId(cashierId)
                .action("TABLE_SESSION_CLOSED")
                .afterState("CLOSED")
                .createdAt(Instant.now())
                .build());

        realtimeEventPublisher.publishAfterCommit("/topic/cashier", "TABLE_SESSION_CLOSED", session.getId().toString(), session.getId().toString(), "1", java.util.Map.of("tableId", table.getId(), "tableName", table.getName()));
        realtimeEventPublisher.publishAfterCommit("/topic/table-sessions/" + session.getId(), "TABLE_SESSION_CLOSED", session.getId().toString(), session.getId().toString(), "1", java.util.Map.of("tableId", table.getId(), "tableName", table.getName()));
    }

    public vn.edu.ute.milktea.dto.table.TableDto.TableSessionResponse getSessionResponse(Long sessionId) {
        TableSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_SESSION_CLOSED, "Không tìm thấy phiên"));
        return vn.edu.ute.milktea.dto.table.TableDto.TableSessionResponse.builder()
                .sessionId(s.getId())
                .tableId(s.getTable().getId())
                .tableName(s.getTable().getName())
                .status(s.getStatus())
                .openedAt(s.getOpenedAt())
                .closedAt(s.getClosedAt())
                .build();
    }

    @Transactional
    public vn.edu.ute.milktea.dto.table.TableDto.TableSessionResponse closeBySessionId(Long sessionId, Long cashierId) {
        TableSession session = sessionRepository.findByIdWithLock(sessionId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TABLE_SESSION_CLOSED, "Không tìm thấy phiên"));
        closeByCashier(session.getTable().getId(), cashierId);
        return getSessionResponse(sessionId);
    }
}
