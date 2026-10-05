package vn.edu.ute.controller.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.dto.customer.*;
import vn.edu.ute.service.order.OrderService;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderApiController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse>
    createGuestOrder(
            @RequestBody
            CreateOrderRequest request
    ) {

        return ResponseEntity.ok(
                orderService
                        .createGuestOrder(request)
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse>
    getGuestOrder(
            @PathVariable Long orderId,

            @RequestHeader("X-Guest-Token")
            String guestToken
    ) {

        return ResponseEntity.ok(
                orderService.getGuestOrder(
                        orderId,
                        guestToken
                )
        );
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponse>
    cancelGuestOrder(
            @PathVariable Long orderId,

            @RequestHeader("X-Guest-Token")
            String guestToken
    ) {

        return ResponseEntity.ok(
                orderService.cancelGuestOrder(
                        orderId,
                        guestToken
                )
        );
    }
}