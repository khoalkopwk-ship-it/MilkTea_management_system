package vn.edu.ute.milktea.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.edu.ute.milktea.entity.voucher.DiscountType;
import vn.edu.ute.milktea.entity.voucher.VoucherUsageStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class VoucherDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherResponse {
        private Long id;
        private String code;
        private String name;
        private String description;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal maxDiscountAmount;
        private BigDecimal minOrderValue;
        private Integer totalUsageLimit;
        private Integer usedCount;
        private Integer remainingUses;
        private Integer usageLimitPerCustomer;
        private Boolean applyAllBranches;
        private List<Long> applicableBranchIds;
        private Instant startDate;
        private Instant endDate;
        private Boolean isActive;
        private Boolean isExpired;
        private Boolean canApply;
        private String formattedDiscountText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateVoucherRequest {
        @NotBlank(message = "Mã voucher không được để trống")
        private String voucherCode;

        private Long branchId;

        @NotNull(message = "Giá trị đơn hàng không được để trống")
        @DecimalMin(value = "0.0", message = "Giá trị đơn hàng không được âm")
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateVoucherResponse {
        private boolean valid;
        private Long voucherId;
        private String voucherCode;
        private String voucherName;
        private BigDecimal discountAmount;
        private BigDecimal originalSubtotal;
        private BigDecimal finalSubtotal;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateVoucherRequest {
        @NotBlank(message = "Mã code voucher không được để trống")
        @Size(min = 3, max = 50, message = "Mã code phải từ 3 đến 50 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã code chỉ được chứa chữ cái, số, gạch dưới hoặc gạch ngang")
        private String code;

        @NotBlank(message = "Tên voucher không được để trống")
        @Size(max = 150, message = "Tên voucher không được quá 150 ký tự")
        private String name;

        private String description;

        @NotNull(message = "Loại giảm giá không được để trống")
        private DiscountType discountType;

        @NotNull(message = "Giá trị giảm không được để trống")
        @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
        private BigDecimal discountValue;

        private BigDecimal maxDiscountAmount;

        @NotNull(message = "Giá trị đơn tối thiểu không được để trống")
        @DecimalMin(value = "0.0", message = "Giá trị đơn tối thiểu không được âm")
        private BigDecimal minOrderValue;

        @NotNull(message = "Tổng số lượt dùng không được để trống")
        @Min(value = 1, message = "Tổng số lượt dùng tối thiểu là 1")
        private Integer totalUsageLimit;

        @NotNull(message = "Số lượt mỗi khách không được để trống")
        @Min(value = 1, message = "Số lượt mỗi khách tối thiểu là 1")
        private Integer usageLimitPerCustomer;

        @Builder.Default
        private Boolean applyAllBranches = true;

        private List<Long> branchIds;

        @NotNull(message = "Ngày bắt đầu không được để trống")
        private Instant startDate;

        @NotNull(message = "Ngày kết thúc không được để trống")
        private Instant endDate;

        @Builder.Default
        private Boolean isActive = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateVoucherRequest {
        @NotBlank(message = "Tên voucher không được để trống")
        @Size(max = 150, message = "Tên voucher không được quá 150 ký tự")
        private String name;

        private String description;

        @NotNull(message = "Loại giảm giá không được để trống")
        private DiscountType discountType;

        @NotNull(message = "Giá trị giảm không được để trống")
        @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
        private BigDecimal discountValue;

        private BigDecimal maxDiscountAmount;

        @NotNull(message = "Giá trị đơn tối thiểu không được để trống")
        @DecimalMin(value = "0.0", message = "Giá trị đơn tối thiểu không được âm")
        private BigDecimal minOrderValue;

        @NotNull(message = "Tổng số lượt dùng không được để trống")
        @Min(value = 1, message = "Tổng số lượt dùng tối thiểu là 1")
        private Integer totalUsageLimit;

        @NotNull(message = "Số lượt mỗi khách không được để trống")
        @Min(value = 1, message = "Số lượt mỗi khách tối thiểu là 1")
        private Integer usageLimitPerCustomer;

        @Builder.Default
        private Boolean applyAllBranches = true;

        private List<Long> branchIds;

        @NotNull(message = "Ngày bắt đầu không được để trống")
        private Instant startDate;

        @NotNull(message = "Ngày kết thúc không được để trống")
        private Instant endDate;

        @Builder.Default
        private Boolean isActive = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherUsageHistoryResponse {
        private Long id;
        private Long voucherId;
        private String voucherCode;
        private String voucherName;
        private Long orderId;
        private Long invoiceId;
        private Long accountId;
        private String customerName;
        private String customerEmail;
        private Long branchId;
        private String branchName;
        private BigDecimal discountAmount;
        private VoucherUsageStatus status;
        private Instant usedAt;
        private Instant refundedAt;
        private String refundReason;
    }
}
