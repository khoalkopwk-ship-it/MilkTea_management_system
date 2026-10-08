package vn.edu.ute.milktea.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesReportResponse {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal completedOrdersTotal;   // Doanh số các đơn hoàn thành
        private BigDecimal actualReceiptsTotal;     // Tổng thực thu
        private BigDecimal actualRefundsTotal;      // Tổng thực hoàn
        private BigDecimal netRevenue;              // Thu ròng = Thực thu - Thực hoàn
        private BigDecimal currentOutstandingDebt;  // Dư nợ các hóa đơn hiệu lực chưa thu
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderReportResponse {
        private LocalDate startDate;
        private LocalDate endDate;
        private long totalOrders;
        private long completedOrders;
        private long cancelledOrders;
        private long tableOrders;
        private long counterOrders;
        private BigDecimal averageOrderValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueReportView {
        private LocalDate fromDate;
        private LocalDate toDate;
        @Builder.Default
        private String currency = "VND";
        private BigDecimal received;
        private BigDecimal refunded;
        private BigDecimal netReceived;
        private BigDecimal completedOrderValue;
        private BigDecimal currentOutstanding;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderReportView {
        private java.util.Map<String, Long> counts;
    }
}
