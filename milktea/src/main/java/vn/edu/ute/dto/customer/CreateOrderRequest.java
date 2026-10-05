package vn.edu.ute.dto.customer;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private Long tableId;
    private List<CreateOrderItemRequest> items;
}