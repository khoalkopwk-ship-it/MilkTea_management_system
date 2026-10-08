package vn.edu.ute.milktea.dto.cancellation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.entity.cancellation.CancellationStatus;
import vn.edu.ute.milktea.entity.cancellation.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class CancellationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCancellationRequest {
        @NotBlank(message = "Lý do hủy đơn không được để trống")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecideCancellationRequest {
        @NotNull(message = "Quyết định không được để trống")
        private Boolean approved;

        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationResponse {
        private Long id;
        private Long orderId;
        private String reason;
        private CancellationStatus status;
        private Instant createdAt;
        private Instant decidedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundResponse {
        private Long id;
        private Long orderId;
        private Long paymentId;
        private BigDecimal amount;
        private String reason;
        private RefundStatus status;
        private Instant approvedAt;
        private Instant refundedAt;
    }
}
