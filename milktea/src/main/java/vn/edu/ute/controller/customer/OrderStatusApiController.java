package vn.edu.ute.controller.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.dto.customer.OrderDetailResponse;
import vn.edu.ute.dto.customer.UpdateOrderStatusRequest;
import vn.edu.ute.service.order.OrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderStatusApiController {

    private final OrderService orderService;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailResponse>
    updateStatus(
            @PathVariable Long orderId,
            @RequestBody
            UpdateOrderStatusRequest request
    ) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderId,
                        request.getStatus()
                )
        );
    }
}