package vn.edu.ute.dto.customer;

import lombok.*;
import vn.edu.ute.enums.order.OrderStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    private OrderStatus status;
}