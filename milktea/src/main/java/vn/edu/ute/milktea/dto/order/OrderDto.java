package vn.edu.ute.milktea.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.milktea.dto.cart.CartDto;
import vn.edu.ute.milktea.entity.order.InvoiceStatus;
import vn.edu.ute.milktea.entity.order.OrderSource;
import vn.edu.ute.milktea.entity.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTableOrderRequest {
        @NotNull(message = "Mã bàn không được để trống")
        private Long tableId;

        private Long expectedSessionId; // null nếu đặt đơn đầu mở phiên

        @NotEmpty(message = "Danh sách món đặt không được rỗng")
        private List<CartDto.CartItemRequest> items;

        private List<CartDto.CartItemRequest> remainingCartItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCounterOrderRequest {
        @NotEmpty(message = "Danh sách món đặt không được rỗng")
        private List<CartDto.CartItemRequest> items;

        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignTableRequest {
        @NotNull(message = "Mã bàn không được để trống")
        private Long tableId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String size;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private Long orderId;
        private Long sessionId;
        private Long tableId;
        private String tableName;
        private OrderSource source;
        private OrderStatus status;
        private Long invoiceId;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private BigDecimal discountPercent;
        private InvoiceStatus invoiceStatus;
        private boolean paid;
        private List<OrderItemResponse> items;
        private Instant createdAt;
        private String guestTableToken;   // Trả về khi đơn đầu tạo phiên
        private String guestCounterToken; // Trả về cho đơn quầy
    }
}
