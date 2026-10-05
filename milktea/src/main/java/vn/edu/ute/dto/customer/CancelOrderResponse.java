package vn.edu.ute.dto.customer;

import lombok.*;
import vn.edu.ute.enums.order.OrderStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderResponse {

    private Long orderId;
    private OrderStatus status;
    private String message;
}