package vn.edu.ute.dto.customer;

import lombok.*;
import vn.edu.ute.enums.order.OrderSource;
import vn.edu.ute.enums.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {

    private Long orderId;
    private String orderCode;
    private Long tableId;
    private OrderSource source;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
}