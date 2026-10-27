package vn.edu.ute.milktea.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.VoucherDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.security.JwtAuthenticationFilter;
import vn.edu.ute.milktea.service.voucher.VoucherService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<VoucherDto.VoucherResponse>>> getActiveVouchers(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "subtotal", required = false) BigDecimal subtotal,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {

        CurrentActor actor = resolveActor(principalActor, request);
        Long accountId = (actor != null && actor.isAuthenticated()) ? actor.getAccountId() : null;

        List<VoucherDto.VoucherResponse> vouchers = voucherService.getActiveVouchersForBranch(branchId, subtotal, accountId);
        return ResponseEntity.ok(ApiResponse.ok(vouchers));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<VoucherDto.ValidateVoucherResponse>> validateVoucher(
            @Valid @RequestBody VoucherDto.ValidateVoucherRequest validateRequest,
            @AuthenticationPrincipal CurrentActor principalActor,
            HttpServletRequest request) {

        CurrentActor actor = resolveActor(principalActor, request);
        Long accountId = (actor != null && actor.isAuthenticated()) ? actor.getAccountId() : null;

        VoucherDto.ValidateVoucherResponse response = voucherService.validateVoucher(
                validateRequest.getVoucherCode(),
                validateRequest.getBranchId(),
                validateRequest.getSubtotal(),
                accountId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private CurrentActor resolveActor(CurrentActor principalActor, HttpServletRequest request) {
        if (principalActor != null) {
            return principalActor;
        }
        if (request != null) {
            Object attr = request.getAttribute(JwtAuthenticationFilter.CURRENT_ACTOR_ATTR);
            if (attr instanceof CurrentActor ca) {
                return ca;
            }
        }
        return null;
    }
}
