package vn.edu.ute.entity.order;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.ute.enums.order.OrderSource;
import vn.edu.ute.enums.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "guest_token", length = 100)
    private String guestToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}