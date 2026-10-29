package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.common.BusinessException;
import vn.edu.ute.milktea.common.ErrorCode;
import vn.edu.ute.milktea.common.ErrorResponse;
import vn.edu.ute.milktea.dto.OrderDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.order.OrderService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping({ "/orders/table", "/customer/orders" })
    public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> createTableOrder(
            @Valid @RequestBody OrderDto.CreateTableOrderRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            HttpServletRequest request) {

        String key = resolveIdempotencyKey(idempotencyKey, xIdempotencyKey);
        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        if (requestBody.getTableToken() == null || requestBody.getTableToken().isBlank()) {
            String token = request.getHeader("X-Table-Session-Token");
            if (token == null || token.isBlank()) token = request.getHeader("X-Table-Token");
            if (token != null && !token.isBlank()) {
                requestBody.setTableToken(token);
            }
        }
        OrderDto.OrderResponse response = orderService.createTableOrder(requestBody, actor, key);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @PostMapping({ "/orders/counter", "/cashier/orders" })
    public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> createCounterOrder(
            @Valid @RequestBody OrderDto.CreateCounterOrderRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            @AuthenticationPrincipal CurrentActor actor) {

        String key = resolveIdempotencyKey(idempotencyKey, xIdempotencyKey);
        OrderDto.OrderResponse response = orderService.createCounterOrder(requestBody, actor, key);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    private String resolveIdempotencyKey(String idempotencyKey, String xIdempotencyKey) {
        boolean hasStandard = idempotencyKey != null && !idempotencyKey.isBlank();
        boolean hasAlias = xIdempotencyKey != null && !xIdempotencyKey.isBlank();
        if (hasStandard && hasAlias && !idempotencyKey.trim().equals(xIdempotencyKey.trim())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "Giá trị header Idempotency-Key và X-Idempotency-Key không đồng nhất");
        }
        return hasStandard ? idempotencyKey.trim() : (hasAlias ? xIdempotencyKey.trim() : null);
    }

    @PostMapping({ "/orders/online", "/customer/orders/online" })
    public ResponseEntity<ErrorResponse> rejectOnlineOrder() {
        return ResponseEntity.status(410).body(ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code(ErrorCode.VALIDATION_FAILED.name())
                        .message("Hệ thống chỉ phục vụ trong không gian quán (TABLE/COUNTER). Chức năng đặt hàng online từ xa đã ngừng hoạt động.")
                        .build())
                .build());
    }

    @GetMapping("/customer/orders")
    public ResponseEntity<ApiResponse<java.util.List<OrderDto.OrderResponse>>> getCustomerOrders(
            HttpServletRequest request) {
        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersForCustomer(actor)));
    }

    @GetMapping("/customer/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> getCustomerOrder(
            @PathVariable("id") Long orderId,
            HttpServletRequest request) {
        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderForCustomer(orderId, actor)));
    }

    @GetMapping({"/cashier/orders", "/admin/orders"})
    public ResponseEntity<ApiResponse<java.util.List<OrderDto.OrderResponse>>> getCashierOrders(
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @RequestParam(value = "status", required = false) vn.edu.ute.milktea.entity.order.OrderStatus status,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "query", required = false) String query,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId;
        if (actor != null && actor.hasRole(vn.edu.ute.milktea.entity.account.Role.MANAGER)) {
            branchId = actor.getBranchId();
        } else if (actor != null && actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            branchId = branchIdParam;
        } else if (actor != null && actor.getBranchId() != null) {
            branchId = actor.getBranchId();
        } else {
            branchId = branchIdParam;
        }
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersForCashier(branchId, status, paymentStatus, tab, query)));
    }

    @GetMapping("/cashier/orders/work-summary")
    public ResponseEntity<ApiResponse<OrderDto.OrderWorkSummaryResponse>> getOrderWorkSummary(
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = (actor != null && actor.getBranchId() != null) ? actor.getBranchId() : branchIdParam;
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderWorkSummary(branchId)));
    }

    @GetMapping("/kitchen/orders")
    public ResponseEntity<ApiResponse<java.util.List<OrderDto.OrderResponse>>> getKitchenOrders(
            @RequestParam(value = "branchId", required = false) Long branchIdParam,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = (actor != null && actor.getBranchId() != null) ? actor.getBranchId() : branchIdParam;
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersForKitchen(branchId)));
    }

    @PostMapping("/cashier/orders/{id}/assign-table")
    public ResponseEntity<ApiResponse<Void>> assignToTable(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody OrderDto.AssignTableRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        orderService.assignCounterOrderToTable(orderId, requestBody.getTableId(), cashierId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/cashier/orders/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(
            @PathVariable("id") Long orderId,
            @AuthenticationPrincipal CurrentActor actor) {

        orderService.confirm(orderId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/kitchen/orders/{id}/start")
    public ResponseEntity<ApiResponse<Void>> startCooking(
            @PathVariable("id") Long orderId,
            @AuthenticationPrincipal CurrentActor actor) {

        orderService.start(orderId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/kitchen/orders/{id}/complete")
    public ResponseEntity<ApiResponse<Void>> completeCooking(
            @PathVariable("id") Long orderId,
            @AuthenticationPrincipal CurrentActor actor) {

        orderService.complete(orderId, actor);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> getOrder(@PathVariable("id") Long orderId,
            @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderForCustomer(orderId, actor)));
    }
}
