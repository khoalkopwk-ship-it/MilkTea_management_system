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
        private String alertStatus;
        private String alertLabel;
        private BigDecimal averageImportPrice;
        private BigDecimal averagePrice;
        private BigDecimal minThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustStockRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        private String name;

        @DecimalMin(value = "0.0", message = "Số lượng tồn không được âm")
        private BigDecimal quantity;

        @DecimalMin(value = "0.0", message = "Giá nhập trung bình không được âm")
        @com.fasterxml.jackson.annotation.JsonAlias({"costPrice", "averageImportPrice", "averagePrice"})
        private BigDecimal averageImportPrice;

        @DecimalMin(value = "0.0", message = "Ngưỡng tối thiểu không được âm")
        @com.fasterxml.jackson.annotation.JsonAlias({"threshold", "minThreshold"})
        private BigDecimal threshold;

        private String reason;

        private Long version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSummaryResponse {
        private long totalCount;
        private long hetTonCount;
        private long tonThapCount;
        private long binhThuongCount;
        private long chuaCauHinhCount;
        private List<StockItemResponse> items;
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
    public static class BatchInputItemRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Lượng nguyên liệu thực tế không được để trống")
        @DecimalMin(value = "0.000001", message = "Lượng nguyên liệu phải lớn hơn 0")
        private BigDecimal actualQuantity;

        private String unit;

        private BigDecimal unitPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePreparationBatchRequest {
        private Long recipeId;
        private Long outputMaterialId;
        private Long recipeVersion;
        private BigDecimal desiredQuantity;
        private BigDecimal actualQuantity;
        private String note;
        private String discrepancyReason;
        private List<BatchInputItemRequest> inputItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreparationBatchItemResponse {
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal recipeQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreparationBatchResponse {
        private Long id;
        private java.time.Instant postedAt;
        private Long recipeId;
        private String recipeName;
        private Long recipeVersion;
        private Long outputMaterialId;
        private String outputMaterialName;
        private BigDecimal expectedQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal differenceQuantity;
        private String unit;
        private String discrepancyReason;
        private String note;
        private String createdByEmail;
        private String createdByName;
        private String status;
        private BigDecimal totalCost;
        private BigDecimal unitCost;
        private BigDecimal ratioVariancePercent;
        private Boolean toleranceWarning;
        private List<PreparationBatchItemResponse> items;
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

    
    // === KẾT CA KHO & HAO HỤT ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSettlementItemRequest {
        @NotNull(message = "Mã nguyên liệu không được để trống")
        private Long materialId;

        @NotNull(message = "Tồn thực tế không được để trống")
        @DecimalMin(value = "0.0", message = "Tồn thực tế không được âm")
        private BigDecimal actualQuantity;

        @NotNull(message = "Lượng loại bỏ không được để trống")
        @DecimalMin(value = "0.0", message = "Lượng loại bỏ không được âm")
        private BigDecimal discardedQuantity;

        @NotNull(message = "Lượng giữ lại không được để trống")
        @DecimalMin(value = "0.0", message = "Lượng giữ lại không được âm")
        private BigDecimal retainedQuantity;

        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateShiftSettlementRequest {
        @NotNull(message = "Vị trí không được để trống")
        private StockLocation location;

        private String note;

        @NotEmpty(message = "Danh sách kiểm kê không được rỗng")
        private List<ShiftSettlementItemRequest> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSettlementItemResponse {
        private Long id;
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal bookQuantity;
        private BigDecimal actualQuantity;
        private BigDecimal discardedQuantity;
        private BigDecimal retainedQuantity;
        private BigDecimal unexplainedDifference;
        private BigDecimal unitCost;
        private BigDecimal lossCost;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSettlementResponse {
        private Long id;
        private StockLocation location;
        private java.time.Instant closedAt;
        private String closedByEmail;
        private String closedByName;
        private BigDecimal totalLossCost;
        private String note;
        private List<ShiftSettlementItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSettlementPreviewItem {
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal bookQuantity;
        private BigDecimal estimatedUnitCost;
        private BigDecimal standardThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSettlementPreviewResponse {
        private StockLocation location;
        private java.time.Instant timestamp;
        private List<ShiftSettlementPreviewItem> items;
    }
}
