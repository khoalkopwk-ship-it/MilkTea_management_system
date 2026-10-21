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
import vn.edu.ute.milktea.entity.inventory.ShiftSettlement;
import vn.edu.ute.milktea.repository.inventory.ShiftSettlementRepository;
import java.util.Map;
import org.springframework.data.domain.PageRequest;

import vn.edu.ute.milktea.repository.order.OrderItemRepository;
import vn.edu.ute.milktea.repository.inventory.ShiftSettlementItemRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import vn.edu.ute.milktea.repository.inventory.StockMovementRepository;
import vn.edu.ute.milktea.entity.inventory.StockMovementType;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;
import vn.edu.ute.milktea.entity.inventory.Stock;
import vn.edu.ute.milktea.entity.inventory.StockMovement;
import vn.edu.ute.milktea.repository.inventory.StockRepository;
import java.util.Comparator;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRepository;
    private final ShiftSettlementRepository shiftSettlementRepository;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OrderItemRepository orderItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ShiftSettlementItemRepository shiftSettlementItemRepository;
    private final StockRepository stockRepository;

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
    
    @Transactional(readOnly = true)
    public ReportDto.DashboardSummary getDashboardSummary(
            LocalDate fromDate,
            LocalDate toDate) {

        // 1. Chuẩn hóa khoảng thời gian
        LocalDate today = LocalDate.now(VIETNAM_ZONE);

        if (toDate == null) {
            toDate = today;
        }

        if (fromDate == null) {
            fromDate = toDate.minusDays(6);
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được lớn hơn ngày kết thúc"
            );
        }

        Instant startInstant = fromDate
                .atStartOfDay(VIETNAM_ZONE)
                .toInstant();

        Instant endInstant = toDate
                .plusDays(1)
                .atStartOfDay(VIETNAM_ZONE)
                .toInstant();

        // 2. Tái sử dụng báo cáo doanh thu
        ReportDto.RevenueReportView revenue =
                getRevenueReport(fromDate, toDate);

        BigDecimal received = revenue.getReceived();
        BigDecimal refunded = revenue.getRefunded();

        BigDecimal netRevenue = received.subtract(refunded);

        // 3. Tổng số đơn hàng trong khoảng ngày
        ReportDto.OrderReportView orderReport =
                getOrderReport(fromDate, toDate);

        Map<String, Long> counts = orderReport.getCounts();

        long totalOrders = counts.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        // 4. Số lượng đơn và số tiền chưa thanh toán
        long unpaidOrders = 0;
        BigDecimal unpaidAmount = BigDecimal.ZERO;

        List<Invoice> invoices = invoiceRepository.findAll();

        for (Invoice invoice : invoices) {

            if (invoice.getStatus() != InvoiceStatus.HIEU_LUC) {
                continue;
            }

            var order = invoice.getOrder();

            if (order == null ||
                    order.getStatus() == OrderStatus.DA_HUY) {
                continue;
            }

            Instant createdAt = order.getCreatedAt();

            if (createdAt == null ||
                    createdAt.isBefore(startInstant) ||
                    !createdAt.isBefore(endInstant)) {
                continue;
            }

            if (!paymentRepository.existsByInvoiceId(invoice.getId())) {
                unpaidOrders++;
                unpaidAmount = unpaidAmount.add(
                        invoice.getTotalAmount()
                );
            }
        }

        // 5. Tổng chi phí hao hụt nguyên liệu cuối ca
        List<ShiftSettlement> settlements =
                shiftSettlementRepository
                        .findByClosedAtBetweenOrderByClosedAtDesc(
                                startInstant,
                                endInstant.minusNanos(1)
                        );

        
        // Chi phí nguyên liệu loại bỏ khi kết ca
        BigDecimal settlementWasteCost = settlements.stream()
                .map(ShiftSettlement::getTotalLossCost)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Chi phí nguyên liệu hư hỏng, hết hạn được báo hủy thủ công
        BigDecimal manualWasteCost = stockMovementRepository.sumManualWasteCost(
                StockMovementType.HAO_HUT,
                startInstant,
                endInstant
        );

        if (manualWasteCost == null) {
            manualWasteCost = BigDecimal.ZERO;
        }

        // Tổng chi phí hao hụt ngoài định mức
        BigDecimal wasteCost = settlementWasteCost.add(manualWasteCost);

        // 6. Trả dữ liệu cho Dashboard
        return ReportDto.DashboardSummary.builder()
                .totalOrders(totalOrders)
                .received(received)
                .refunded(refunded)
                .netRevenue(netRevenue)
                .unpaidOrders(unpaidOrders)
                .unpaidAmount(unpaidAmount)
                .wasteCost(wasteCost)
                .unpricedWasteCount(stockMovementRepository.countUnpricedManualWaste(
                        StockMovementType.HAO_HUT, startInstant, endInstant))
                .build();
    }
    //phương thức thống kê doanh thu theo ngày 
    @Transactional(readOnly = true)
    public List<ReportDto.RevenueByDay> getDashboardRevenueByDay(
            LocalDate fromDate,
            LocalDate toDate) {

        Instant start = fromDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant end = toDate.plusDays(1)
                .atStartOfDay(VIETNAM_ZONE).toInstant();

        Map<LocalDate, BigDecimal> receivedByDay = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> refundedByDay = new LinkedHashMap<>();

        for (LocalDate day = fromDate; !day.isAfter(toDate);
                day = day.plusDays(1)) {
            receivedByDay.put(day, BigDecimal.ZERO);
            refundedByDay.put(day, BigDecimal.ZERO);
        }

        for (Payment payment : paymentRepository.findByPaidAtBetween(start, end)) {
            if (payment.getPaidAt() == null || payment.getAmount() == null) {
                continue;
            }

            LocalDate day = payment.getPaidAt()
                    .atZone(VIETNAM_ZONE).toLocalDate();

            receivedByDay.merge(day, payment.getAmount(), BigDecimal::add);
        }

        for (RefundRequest refund : refundRepository.findCompletedRefundsBetween(start, end)) {
            if (refund.getRefundedAt() == null || refund.getAmount() == null) {
                continue;
            }

            LocalDate day = refund.getRefundedAt()
                    .atZone(VIETNAM_ZONE).toLocalDate();

            refundedByDay.merge(day, refund.getAmount(), BigDecimal::add);
        }

        List<ReportDto.RevenueByDay> result = new ArrayList<>();

        for (LocalDate day : receivedByDay.keySet()) {
            BigDecimal received = receivedByDay.get(day);
            BigDecimal refunded = refundedByDay.get(day);

            result.add(ReportDto.RevenueByDay.builder()
                    .date(day)
                    .received(received)
                    .refunded(refunded)
                    .netRevenue(received.subtract(refunded))
                    .build());
        }

        return result;
    }
//phương thức thống kê trạng thái đơn

    @Transactional(readOnly = true)
    public List<ReportDto.OrderStatusStatistic> getDashboardOrderStatuses(
            LocalDate fromDate,
            LocalDate toDate) {

        Instant start = fromDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant end = toDate.plusDays(1)
                .atStartOfDay(VIETNAM_ZONE).toInstant();

        Map<OrderStatus, Long> counts = new LinkedHashMap<>();

        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }

        for (Object[] row : orderRepository.countOrdersByStatusBetween(start, end)) {
            counts.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }

        Map<OrderStatus, String> labels = Map.of(
                OrderStatus.CHO_THANH_TOAN, "Chờ thanh toán",
                OrderStatus.CHO_XAC_NHAN, "Chờ xác nhận",
                OrderStatus.CHO_CHE_BIEN, "Chờ chế biến",
                OrderStatus.DANG_CHE_BIEN, "Đang chế biến",
                OrderStatus.HOAN_THANH, "Hoàn thành",
                OrderStatus.DA_HUY, "Đã hủy"
        );

        List<ReportDto.OrderStatusStatistic> result = new ArrayList<>();

        for (var entry : counts.entrySet()) {
            result.add(ReportDto.OrderStatusStatistic.builder()
                    .status(entry.getKey().name())
                    .statusLabel(labels.getOrDefault(
                            entry.getKey(), entry.getKey().name()))
                    .count(entry.getValue())
                    .build());
        }

        return result;
    }
//top 5 món bán chạy 

    @Transactional(readOnly = true)
    public List<ReportDto.TopSellingProduct> getDashboardTopProducts(
            LocalDate fromDate,
            LocalDate toDate) {

        Instant start = fromDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant end = toDate.plusDays(1)
                .atStartOfDay(VIETNAM_ZONE).toInstant();

        return orderItemRepository.findTopSellingProducts(
                start,
                end,
                OrderStatus.DA_HUY,
                PageRequest.of(0, 5)
        ).stream()
        .map(row -> ReportDto.TopSellingProduct.builder()
                .productId(row[0] == null
                        ? null : ((Number) row[0]).longValue())
                .productName((String) row[1])
                .quantitySold(((Number) row[2]).longValue())
                .build())
        .toList();
    }
//top 5 nguyên liệu hao hụt ngoài định mức nhiều nhất 

    
@Transactional(readOnly = true)
public List<ReportDto.TopWasteMaterial> getDashboardTopWasteMaterials(
        LocalDate fromDate,
        LocalDate toDate) {

    Instant start = fromDate.atStartOfDay(VIETNAM_ZONE).toInstant();
    Instant end = toDate.plusDays(1)
            .atStartOfDay(VIETNAM_ZONE).toInstant();

    Map<Long, ReportDto.TopWasteMaterial> totals = new HashMap<>();

    // 1. Hao hụt khi kết ca
    List<Object[]> settlementRows =
            shiftSettlementItemRepository.findAllWasteMaterialsBetween(
                    start, end);

    for (Object[] row : settlementRows) {
        Long materialId = ((Number) row[0]).longValue();
        String materialName = (String) row[1];
        String unit = (String) row[2];

        BigDecimal quantity = row[3] == null
                ? BigDecimal.ZERO : (BigDecimal) row[3];

        BigDecimal cost = row[4] == null
                ? BigDecimal.ZERO : (BigDecimal) row[4];

        totals.put(materialId, ReportDto.TopWasteMaterial.builder()
                .materialId(materialId)
                .materialName(materialName)
                .unit(unit)
                .discardedQuantity(quantity)
                .lossCost(cost)
                .build());
    }

    // 2. Hao hụt thủ công: nguyên liệu hư hỏng, hết hạn
    List<StockMovement> manualWaste =
            stockMovementRepository.findManualWasteBetween(
                    StockMovementType.HAO_HUT,
                    start,
                    end
            );

    for (StockMovement movement : manualWaste) {

        // Bản ghi cũ chưa có giá vốn thì không tự ước tính.
        if (movement.getLossCost() == null) {
            continue;
        }

        Long materialId = movement.getMaterial().getId();
        String materialName = movement.getMaterial().getName();
        String unit = movement.getMaterial().getUnit();

        BigDecimal quantity = movement.getDeltaQuantity().abs();
        BigDecimal cost = movement.getLossCost();

        ReportDto.TopWasteMaterial existing = totals.get(materialId);

        if (existing == null) {
            totals.put(materialId, ReportDto.TopWasteMaterial.builder()
                    .materialId(materialId)
                    .materialName(materialName)
                    .unit(unit)
                    .discardedQuantity(quantity)
                    .lossCost(cost)
                    .build());
        } else {
            existing.setDiscardedQuantity(
                    existing.getDiscardedQuantity().add(quantity)
            );
            existing.setLossCost(
                    existing.getLossCost().add(cost)
            );
        }
    }

    // 3. Xếp hạng sau khi đã gộp toàn bộ hai nguồn
    return totals.values().stream()
            .sorted(
                    Comparator.comparing(
                            ReportDto.TopWasteMaterial::getLossCost
                    ).reversed()
                    .thenComparing(
                            ReportDto.TopWasteMaterial::getMaterialId
                    )
            )
            .limit(5)
            .toList();
}

//lấy trạng thái cảnh báo hiện tại
@Transactional(readOnly = true)
public ReportDto.DashboardAlerts getDashboardAlerts() {

    long outOfStockCount = 0;
    long lowStockCount = 0;
    List<ReportDto.DashboardStockAlert> stockItems = new ArrayList<>();

    for (Stock stock : stockRepository.findAll()) {
        if (stock.getMaterial() == null ||
                !Boolean.TRUE.equals(stock.getMaterial().getActive())) {
            continue;
        }

        BigDecimal quantity = stock.getQuantity() == null
                ? BigDecimal.ZERO : stock.getQuantity();

        BigDecimal threshold = stock.getThreshold();

        String status = null;
        if (quantity.signum() <= 0) {
            status = "HET_HANG";
            outOfStockCount++;
        } else if (threshold != null &&
                threshold.signum() > 0 &&
                quantity.compareTo(threshold) < 0) {
            status = "SAP_HET";
            lowStockCount++;
        }
        if (status != null) {
            stockItems.add(ReportDto.DashboardStockAlert.builder()
                    .materialId(stock.getMaterial().getId())
                    .materialName(stock.getMaterial().getName())
                    .unit(stock.getMaterial().getUnit())
                    .location(stock.getId().getLocation().name())
                    .quantity(quantity)
                    .threshold(threshold)
                    .status(status)
                    .build());
        }
    }
    stockItems.sort(Comparator.comparing(ReportDto.DashboardStockAlert::getMaterialName)
            .thenComparing(ReportDto.DashboardStockAlert::getLocation));

    long pendingRefundCount = refundRepository
            .findByStatus(RefundStatus.CHO_DUYET)
            .size();

    var settlements = shiftSettlementRepository
            .findAllByOrderByClosedAtDesc();

    LocalDateTime lastSettlementAt = null;

    if (!settlements.isEmpty() &&
            settlements.get(0).getClosedAt() != null) {

        lastSettlementAt = LocalDateTime.ofInstant(
                settlements.get(0).getClosedAt(),
                VIETNAM_ZONE
        );
    }

    return ReportDto.DashboardAlerts.builder()
            .outOfStockCount(outOfStockCount)
            .lowStockCount(lowStockCount)
            .pendingRefundCount(pendingRefundCount)
            .stockItems(stockItems)
            .lastSettlementAt(lastSettlementAt)
            .build();
}


@Transactional(readOnly = true)
public ReportDto.DashboardResponse getDashboard(
        LocalDate fromDate,
        LocalDate toDate) {

    LocalDate today = LocalDate.now(VIETNAM_ZONE);

    if (toDate == null) {
        toDate = today;
    }

    if (fromDate == null) {
        fromDate = toDate.minusDays(6);
    }

    if (fromDate.isAfter(toDate)) {
        throw new IllegalArgumentException(
                "Ngày bắt đầu không được lớn hơn ngày kết thúc"
        );
    }

    return ReportDto.DashboardResponse.builder()
            .fromDate(fromDate)
            .toDate(toDate)
            .summary(getDashboardSummary(fromDate, toDate))
            .revenueByDay(getDashboardRevenueByDay(fromDate, toDate))
            .ordersByStatus(getDashboardOrderStatuses(fromDate, toDate))
            .topProducts(getDashboardTopProducts(fromDate, toDate))
            .topWasteMaterials(getDashboardTopWasteMaterials(fromDate, toDate))
            .alerts(getDashboardAlerts())
            .build();
}

}
