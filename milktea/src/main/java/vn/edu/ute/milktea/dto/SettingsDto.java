package vn.edu.ute.milktea.dto;

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
        private String logoUrl;
        private String logoPublicId;
        private BigDecimal discountPercent;
        private String discountType; // PERCENT, FIXED_AMOUNT
        private BigDecimal discountValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogoImageView {
        private String logoUrl;
        private String logoPublicId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSettingsRequest {
        @NotBlank(message = "Tên quán không được để trống")
        private String shopName;

        @NotBlank(message = "Địa chỉ quán không được để trống")
        private String address;

        private String logoUrl;
        private String logoPublicId;
        private String bankName;
        private String bankAccountNumber;
        private String bankAccountHolder;

        private String discountType; // PERCENT, FIXED_AMOUNT
        private BigDecimal discountValue;
        private BigDecimal discountPercent; // legacy compatibility
    }
}
