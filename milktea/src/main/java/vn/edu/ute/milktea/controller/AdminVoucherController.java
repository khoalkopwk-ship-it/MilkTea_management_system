package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.VoucherDto;
import vn.edu.ute.milktea.service.voucher.VoucherService;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VoucherDto.VoucherResponse>>> listVouchers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<VoucherDto.VoucherResponse> response = voucherService.searchVouchers(keyword, isActive, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherDto.VoucherResponse>> getVoucher(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.getVoucherById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherDto.VoucherResponse>> createVoucher(
            @Valid @RequestBody VoucherDto.CreateVoucherRequest request) {
        VoucherDto.VoucherResponse response = voucherService.createVoucher(request);
        return ResponseEntity.status(201).body(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherDto.VoucherResponse>> updateVoucher(
            @PathVariable("id") Long id,
            @Valid @RequestBody VoucherDto.UpdateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.updateVoucher(id, request)));
    }

    @RequestMapping(value = "/{id}/toggle-status", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Void>> toggleVoucherStatus(@PathVariable("id") Long id) {
        voucherService.toggleVoucherStatus(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<Page<VoucherDto.VoucherUsageHistoryResponse>>> getUsageHistory(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(validPage, validSize);

        return ResponseEntity.ok(ApiResponse.ok(voucherService.getVoucherUsageHistory(id, pageRequest)));
    }
}
