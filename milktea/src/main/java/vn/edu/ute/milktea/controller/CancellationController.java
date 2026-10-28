package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.CancellationDto;
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

    @GetMapping({"/cashier/cancellation-requests", "/admin/cancellation-requests"})
    public ResponseEntity<ApiResponse<java.util.List<CancellationDto.CancellationResponse>>> getCancellationRequests(
            @RequestParam(value = "status", required = false) vn.edu.ute.milktea.entity.cancellation.CancellationStatus status,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = (actor != null && !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) ? actor.getBranchId() : null;
        return ResponseEntity.ok(ApiResponse.ok(cancellationService.getCancellationRequests(status, branchId)));
    }

    @PostMapping({"/cashier/cancellation-requests/{id}/decision", "/admin/cancellation-requests/{id}/decision"})
    public ResponseEntity<ApiResponse<Void>> decideCancellation(
            @PathVariable("id") Long requestId,
            @Valid @RequestBody CancellationDto.DecideCancellationRequest requestBody,
            @AuthenticationPrincipal CurrentActor actor) {

        cancellationService.decideCancellation(requestId, requestBody, actor);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/cashier/cancellation-requests/{id}/refund")
    public ResponseEntity<ApiResponse<CancellationDto.RefundResponse>> requestRefund(
            @PathVariable("id") Long id, @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.requestFromCancellation(id, actor)));
    }

    @PostMapping("/cashier/orders/{id}/refund-bank-request")
    public ResponseEntity<ApiResponse<CancellationDto.RefundResponse>> requestBankRefundByOrder(
            @PathVariable("id") Long orderId, @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.requestBankRefund(orderId, actor)));
    }

    @PostMapping("/cashier/cancellation-requests/{id}/refund-bank-request")
    public ResponseEntity<ApiResponse<CancellationDto.RefundResponse>> requestBankRefundByCancellation(
            @PathVariable("id") Long cancellationId, @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.requestFromCancellation(cancellationId, actor)));
    }

    @PostMapping("/cashier/orders/{id}/refund-cash")
    public ResponseEntity<ApiResponse<CancellationDto.RefundResponse>> refundCashByOrder(
            @PathVariable("id") Long orderId, @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.refundCash(orderId, actor)));
    }

    @PostMapping("/cashier/cancellation-requests/{id}/refund-cash")
    public ResponseEntity<ApiResponse<CancellationDto.RefundResponse>> refundCashByCancellation(
            @PathVariable("id") Long cancellationId, @AuthenticationPrincipal CurrentActor actor) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.requestFromCancellation(cancellationId, actor)));
    }

    @GetMapping("/admin/refunds")
    public ResponseEntity<ApiResponse<java.util.List<CancellationDto.RefundResponse>>> getAdminRefunds(
            @RequestParam(value = "status", required = false) vn.edu.ute.milktea.entity.cancellation.RefundStatus status,
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = (actor != null && !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) ? actor.getBranchId() : null;
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefunds(status, branchId)));
    }

    @PostMapping("/admin/refunds/{id}/decision")
    public ResponseEntity<ApiResponse<Void>> decideRefund(
            @PathVariable("id") Long refundId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CurrentActor actor) {

        if (actor == null || !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) {
            throw vn.edu.ute.milktea.common.BusinessException.forbidden(
                    vn.edu.ute.milktea.common.ErrorCode.ACCESS_DENIED,
                    "Chỉ Quản trị viên (ADMIN) mới có quyền duyệt hoàn tiền ngân hàng");
        }

        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String reason = (String) body.get("reason");
        String reference = (String) body.get("reference");
        Long adminId = actor.getAccountId();

        refundService.decideRefund(refundId, approved, reason, reference, adminId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/cashier/refunds")
    public ResponseEntity<ApiResponse<java.util.List<CancellationDto.RefundResponse>>> getCashierRefunds(
            @AuthenticationPrincipal CurrentActor actor) {
        Long branchId = (actor != null && !actor.hasRole(vn.edu.ute.milktea.entity.account.Role.ADMIN)) ? actor.getBranchId() : null;
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefunds(vn.edu.ute.milktea.entity.cancellation.RefundStatus.DA_DUYET, branchId)));
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
