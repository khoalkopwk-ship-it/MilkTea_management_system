package vn.edu.ute.milktea.dto.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class SettingsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopInfoResponse {
        private String shopName;
        private String address;
        private String bankName;
        private String bankAccountNumber;
        private String bankAccountHolder;
        private BigDecimal discountPercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSettingsRequest {
        @NotBlank(message = "Tên quán không được để trống")
        private String shopName;

        @NotBlank(message = "Địa chỉ quán không được để trống")
        private String address;

        private String bankName;
        private String bankAccountNumber;
        private String bankAccountHolder;

        @NotNull(message = "Tỷ lệ giảm giá không được để trống")
        @DecimalMin(value = "0.00", message = "Tỷ lệ giảm tối thiểu là 0%")
        @DecimalMax(value = "100.00", message = "Tỷ lệ giảm tối đa là 100%")
        private BigDecimal discountPercent;
    }
}
