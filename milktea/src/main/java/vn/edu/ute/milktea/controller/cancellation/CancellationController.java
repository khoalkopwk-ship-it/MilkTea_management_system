package vn.edu.ute.milktea.controller.cancellation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.cancellation.CancellationDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.cancellation.CancellationService;
import vn.edu.ute.milktea.service.cancellation.RefundService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CancellationController {

    private final CancellationService cancellationService;
    private final RefundService refundService;

    @PostMapping({"/orders/{id}/cancellation-requests", "/customer/orders/{id}/cancellation-requests"})
    public ResponseEntity<ApiResponse<CancellationDto.CancellationResponse>> requestCancellation(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody CancellationDto.CreateCancellationRequest requestBody,
            HttpServletRequest request) {

        CurrentActor actor = (CurrentActor) request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
        CancellationDto.CancellationResponse response = cancellationService.requestCancellation(orderId, requestBody, actor);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @GetMapping("/cashier/cancellation-requests")
    public ResponseEntity<ApiResponse<java.util.List<CancellationDto.CancellationResponse>>> getCancellationRequests(
            @RequestParam(value = "status", required = false) vn.edu.ute.milktea.entity.cancellation.CancellationStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(cancellationService.getCancellationRequests(status)));
    }

    @PostMapping("/cashier/cancellation-requests/{id}/decision")
    public ResponseEntity<ApiResponse<Void>> decideCancellation(
            @PathVariable("id") Long requestId,
            @Valid @RequestBody CancellationDto.DecideCancellationRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        cancellationService.decideCancellation(requestId, requestBody, cashierId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/admin/refunds")
    public ResponseEntity<ApiResponse<java.util.List<CancellationDto.RefundResponse>>> getAdminRefunds(
            @RequestParam(value = "status", required = false) vn.edu.ute.milktea.entity.cancellation.RefundStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefunds(status)));
    }

    @PostMapping("/admin/refunds/{id}/decision")
    public ResponseEntity<ApiResponse<Void>> decideRefund(
            @PathVariable("id") Long refundId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CurrentActor actor) {

        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String reason = (String) body.get("reason");
        Long adminId = actor != null ? actor.getAccountId() : null;

        refundService.decideRefund(refundId, approved, reason, adminId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/cashier/refunds")
    public ResponseEntity<ApiResponse<java.util.List<CancellationDto.RefundResponse>>> getCashierRefunds() {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefunds(vn.edu.ute.milktea.entity.cancellation.RefundStatus.DA_DUYET)));
    }

    @PostMapping({"/cashier/refunds/{id}/record-refund", "/cashier/refunds/{id}/execute"})
    public ResponseEntity<ApiResponse<Void>> recordRefund(
            @PathVariable("id") Long refundId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CurrentActor actor) {

        String reference = body != null ? body.get("reference") : null;
        Long cashierId = actor != null ? actor.getAccountId() : null;

        refundService.recordRefundPayment(refundId, reference, cashierId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
