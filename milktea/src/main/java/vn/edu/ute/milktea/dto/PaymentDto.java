package vn.edu.ute.milktea.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordPaymentRequest {
        @NotNull(message = "Phương thức thanh toán không được để trống")
        private PaymentMethod method; // CASH, BANK_TRANSFER

        private String reference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentNoticeRequest {
        @NotNull(message = "Phương thức thanh toán không được để trống")
        private PaymentMethod method;

        private String reference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private Long paymentId;
        private Long invoiceId;
        private PaymentMethod method;
        private BigDecimal amount;
        private String reference;
        private String recordedByEmail;
        private Instant paidAt;
    }
}
