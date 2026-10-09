package vn.edu.ute.milktea.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.ute.milktea.dto.ReportDto;
import vn.edu.ute.milktea.entity.cancellation.RefundRequest;
import vn.edu.ute.milktea.entity.order.Invoice;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.order.OrderStatus;
import vn.edu.ute.milktea.entity.payment.Payment;
import vn.edu.ute.milktea.repository.cancellation.RefundRequestRepository;
import vn.edu.ute.milktea.repository.order.InvoiceRepository;
import vn.edu.ute.milktea.repository.order.OrderRepository;
import vn.edu.ute.milktea.repository.payment.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRepository;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional(readOnly = true)
    public ReportDto.SalesReportResponse getSalesReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now(VIETNAM_ZONE).minusDays(30);
        if (endDate == null) endDate = LocalDate.now(VIETNAM_ZONE).plusDays(1);

        Instant startInstant = startDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant endInstant = endDate.atStartOfDay(VIETNAM_ZONE).toInstant();

        // 1. Thực thu (Payments completed trong khoảng)
        List<Payment> payments = paymentRepository.findByPaidAtBetween(startInstant, endInstant);
        BigDecimal actualReceiptsTotal = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Thực hoàn (Refunds DA_HOAN trong khoảng)
        List<RefundRequest> refunds = refundRepository.findCompletedRefundsBetween(startInstant, endInstant);
        BigDecimal actualRefundsTotal = refunds.stream()
                .map(RefundRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Thu ròng = Thực thu - Thực hoàn
        BigDecimal netRevenue = actualReceiptsTotal.subtract(actualRefundsTotal);

        // 4. Doanh số các đơn hoàn thành
        var completedOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(OrderStatus.HOAN_THANH));
        BigDecimal completedOrdersTotal = BigDecimal.ZERO;
        for (var ord : completedOrders) {
            if (ord.getCompletedAt() != null &&
                    !ord.getCompletedAt().isBefore(startInstant) &&
                    ord.getCompletedAt().isBefore(endInstant)) {
                Invoice inv = invoiceRepository.findByOrderId(ord.getId()).orElse(null);
                if (inv != null && inv.getStatus() == InvoiceStatus.HIEU_LUC) {
                    completedOrdersTotal = completedOrdersTotal.add(inv.getTotalAmount());
                }
            }
        }

        // 5. Dư nợ hiện tại (Tất cả hóa đơn HIEU_LUC chưa có payment)
        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal currentOutstandingDebt = BigDecimal.ZERO;
        for (Invoice inv : allInvoices) {
            if (inv.getStatus() == InvoiceStatus.HIEU_LUC) {
                if (!paymentRepository.existsByInvoiceId(inv.getId())) {
                    currentOutstandingDebt = currentOutstandingDebt.add(inv.getTotalAmount());
                }
            }
        }

        return ReportDto.SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .completedOrdersTotal(completedOrdersTotal)
                .actualReceiptsTotal(actualReceiptsTotal)
                .actualRefundsTotal(actualRefundsTotal)
                .netRevenue(netRevenue)
                .currentOutstandingDebt(currentOutstandingDebt)
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.RevenueReportView getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now(VIETNAM_ZONE).minusDays(30);
        if (toDate == null) toDate = LocalDate.now(VIETNAM_ZONE);

        Instant startInstant = fromDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant endInstant = toDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        // 1. Thực thu (Payments completed trong khoảng)
        List<Payment> payments = paymentRepository.findByPaidAtBetween(startInstant, endInstant);
        BigDecimal received = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Thực hoàn (Refunds DA_HOAN trong khoảng)
        List<RefundRequest> refunds = refundRepository.findCompletedRefundsBetween(startInstant, endInstant);
        BigDecimal refunded = refunds.stream()
                .map(RefundRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Thu ròng = Thực thu - Thực hoàn
        BigDecimal netReceived = received.subtract(refunded);

        // 4. Doanh số đơn hoàn thành (completedOrderValue)
        var completedOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(OrderStatus.HOAN_THANH));
        BigDecimal completedOrderValue = BigDecimal.ZERO;
        for (var ord : completedOrders) {
            if (ord.getCompletedAt() != null &&
                    !ord.getCompletedAt().isBefore(startInstant) &&
                    ord.getCompletedAt().isBefore(endInstant)) {
                Invoice inv = invoiceRepository.findByOrderId(ord.getId()).orElse(null);
                if (inv != null && inv.getStatus() == InvoiceStatus.HIEU_LUC) {
                    completedOrderValue = completedOrderValue.add(inv.getTotalAmount());
                }
            }
        }

        // 5. Dư nợ hiện tại (Tất cả hóa đơn HIEU_LUC chưa có payment)
        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal currentOutstanding = BigDecimal.ZERO;
        for (Invoice inv : allInvoices) {
            if (inv.getStatus() == InvoiceStatus.HIEU_LUC) {
                if (!paymentRepository.existsByInvoiceId(inv.getId())) {
                    currentOutstanding = currentOutstanding.add(inv.getTotalAmount());
                }
            }
        }

        return ReportDto.RevenueReportView.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .currency("VND")
                .received(received)
                .refunded(refunded)
                .netReceived(netReceived)
                .completedOrderValue(completedOrderValue)
                .currentOutstanding(currentOutstanding)
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.OrderReportView getOrderReport(LocalDate fromDate, LocalDate toDate) {
        var allOrders = orderRepository.findAll();
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            counts.put(st.name(), 0L);
        }

        Instant startInstant = fromDate != null ? fromDate.atStartOfDay(VIETNAM_ZONE).toInstant() : null;
        Instant endInstant = toDate != null ? toDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant() : null;

        for (var ord : allOrders) {
            Instant t = ord.getCreatedAt();
            if (startInstant != null && t.isBefore(startInstant)) continue;
            if (endInstant != null && !t.isBefore(endInstant)) continue;

            String stName = ord.getStatus().name();
            counts.put(stName, counts.getOrDefault(stName, 0L) + 1L);
        }

        return ReportDto.OrderReportView.builder()
                .counts(counts)
                .build();
    }
}
