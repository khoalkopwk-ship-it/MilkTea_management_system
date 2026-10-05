package vn.edu.ute.dto.customer;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemRequest {

    private Long productId;
    private Long sizeId;
    private Integer quantity;
    private String note;
}