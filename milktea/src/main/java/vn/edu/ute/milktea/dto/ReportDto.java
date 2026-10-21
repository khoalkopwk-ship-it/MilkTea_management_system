package vn.edu.ute.milktea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public class ReportDto {


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
    
    // ==================== DASHBOARD ADMIN ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private LocalDate fromDate;
        private LocalDate toDate;
        private DashboardSummary summary;
        private List<RevenueByDay> revenueByDay;
        private List<OrderStatusStatistic> ordersByStatus;
        private List<TopSellingProduct> topProducts;
        private List<TopWasteMaterial> topWasteMaterials;
        private DashboardAlerts alerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSummary {
        private long totalOrders;
        private BigDecimal received;
        private BigDecimal refunded;
        private BigDecimal netRevenue;
        private long unpaidOrders;
        private BigDecimal unpaidAmount;
        private BigDecimal wasteCost;
        private long unpricedWasteCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueByDay {
        private LocalDate date;
        private BigDecimal received;
        private BigDecimal refunded;
        private BigDecimal netRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusStatistic {
        private String status;
        private String statusLabel;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingProduct {
        private Long productId;
        private String productName;
        private long quantitySold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopWasteMaterial {
        private Long materialId;
        private String materialName;
        private BigDecimal discardedQuantity;
        private String unit;
        private BigDecimal lossCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardAlerts {
        private long outOfStockCount;
        private long lowStockCount;
        private long pendingRefundCount;
        private LocalDateTime lastSettlementAt;
        private List<DashboardStockAlert> stockItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStockAlert {
        private Long materialId;
        private String materialName;
        private String unit;
        private String location;
        private BigDecimal quantity;
        private BigDecimal threshold;
        private String status;
    }

}
