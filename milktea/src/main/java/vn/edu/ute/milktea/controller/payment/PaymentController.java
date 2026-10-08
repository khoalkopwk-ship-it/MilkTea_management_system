package vn.edu.ute.milktea.controller.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.payment.PaymentDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.payment.PaymentService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/cashier/orders/{id}/payments")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentResponse>> recordOrderPayment(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody PaymentDto.RecordPaymentRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        PaymentDto.PaymentResponse response = paymentService.recordReceiptForOrder(orderId, requestBody, cashierId, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @PostMapping("/cashier/invoices/{id}/payments")
    public ResponseEntity<ApiResponse<PaymentDto.PaymentResponse>> recordInvoicePayment(
            @PathVariable("id") Long invoiceId,
            @Valid @RequestBody PaymentDto.RecordPaymentRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CurrentActor actor) {

        Long cashierId = actor != null ? actor.getAccountId() : null;
        PaymentDto.PaymentResponse response = paymentService.recordReceipt(invoiceId, requestBody, cashierId, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @PostMapping("/orders/{id}/payment-notices")
    public ResponseEntity<ApiResponse<Void>> createOrderNotice(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody PaymentDto.PaymentNoticeRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        paymentService.createNoticeForOrder(orderId, requestBody, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/customer/invoices/{id}/payment-notices")
    public ResponseEntity<ApiResponse<Void>> createNotice(
            @PathVariable("id") Long invoiceId,
            @Valid @RequestBody PaymentDto.PaymentNoticeRequest requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        paymentService.createNotice(invoiceId, requestBody, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
