package vn.edu.ute.milktea.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.inventory.MaterialType;
import vn.edu.ute.milktea.entity.inventory.StockLocation;

import java.math.BigDecimal;
import java.util.List;

public class InventoryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItemResponse {
        private Long materialId;
        private String materialName;
        private MaterialType type;
        private String unit;
        private StockLocation location;
        private BigDecimal quantity;
        private BigDecimal threshold;
        private boolean isLowStock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateThresholdRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Vị trí kho không được để trống")
        private StockLocation location;

        @NotNull(message = "Ngưỡng cảnh báo không được để trống")
        @DecimalMin(value = "0.0", message = "Ngưỡng không được âm")
        private BigDecimal threshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockIssueItemRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.001", message = "Số lượng xuất phải lớn hơn 0")
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateStockIssueRequest {
        @NotNull(message = "Lý do xuất kho không được để trống")
        private String reason;

        @NotEmpty(message = "Danh sách nguyên liệu xuất không được rỗng")
        private List<StockIssueItemRequest> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePreparationBatchRequest {
        @NotNull(message = "Mã công thức sơ chế không được để trống")
        private Long recipeId;

        @NotNull(message = "Sản lượng thực thu không được để trống")
        @DecimalMin(value = "0.0", message = "Sản lượng không được âm")
        private BigDecimal actualQuantity;

        private String discrepancyReason;
    }

    // === NHẬP KHO ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportItemRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.001", message = "Số lượng nhập phải lớn hơn 0")
        private BigDecimal quantity;

        @NotNull(message = "Đơn giá không được để trống")
        @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
        private BigDecimal unitPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateImportReceiptRequest {
        private String reason;
        private String supplier;

        @NotEmpty(message = "Danh sách nguyên liệu nhập không được rỗng")
        private List<ImportItemRequest> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportReceiptResponse {
        private Long id;
        private String reason;
        private String supplier;
        private String status;
        private java.time.Instant createdAt;
        private String createdByEmail;
    }

    // === HAO HỤT / HỦY KHO ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WasteItemRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.001", message = "Số lượng hủy phải lớn hơn 0")
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateWasteRequest {
        @NotNull(message = "Vị trí kho không được để trống")
        private StockLocation location;

        @NotNull(message = "Lý do hủy/hao hụt không được để trống")
        private String reason;

        @NotEmpty(message = "Danh sách nguyên liệu không được rỗng")
        private List<WasteItemRequest> items;
    }

    // === BIẾN ĐỘNG KHO (MOVEMENTS) ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMovementResponse {
        private Long id;
        private Long materialId;
        private String materialName;
        private StockLocation location;
        private String type;
        private BigDecimal deltaQuantity;
        private BigDecimal beforeQuantity;
        private BigDecimal afterQuantity;
        private String sourceType;
        private Long sourceId;
        private String reason;
        private java.time.Instant createdAt;
    }

    // === SỰ CỐ & PHA BÙ (INCIDENTS & REMAKE) ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentRequest {
        @NotNull(message = "Lý do sự cố không được để trống")
        private String reason;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentResponse {
        private Long id;
        private Long orderId;
        private String reason;
        private java.time.Instant createdAt;
        private String createdByEmail;
        private boolean resolved;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalConsumptionItemRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.001", message = "Số lượng pha bù phải lớn hơn 0")
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalConsumptionRequest {
        private Long incidentId;

        @NotNull(message = "Lý do pha bù không được để trống")
        private String reason;

        @NotEmpty(message = "Danh sách nguyên liệu pha bù không được rỗng")
        private List<AdditionalConsumptionItemRequest> items;
    }
}
