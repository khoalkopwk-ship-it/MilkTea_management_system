package vn.edu.ute.dto.customer;

import lombok.*;
import vn.edu.ute.enums.order.OrderStatus;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderResponse {

    private Long orderId;
    private String orderCode;
    private String guestToken;
    private OrderStatus status;
    private BigDecimal totalAmount;
}