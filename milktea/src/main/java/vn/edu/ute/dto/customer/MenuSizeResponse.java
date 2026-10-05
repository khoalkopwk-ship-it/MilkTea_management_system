package vn.edu.ute.dto.customer;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuSizeResponse {

    private Long id;
    private String name;
    private BigDecimal extraPrice;
}