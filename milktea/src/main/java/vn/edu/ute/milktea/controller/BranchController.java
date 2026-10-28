package vn.edu.ute.milktea.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.BranchDto;
import vn.edu.ute.milktea.security.CurrentActor;
import vn.edu.ute.milktea.service.branch.BranchService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping({"/branches", "/public/branches"})
    public ResponseEntity<ApiResponse<List<BranchDto.BranchResponse>>> getActiveBranches() {
        return ResponseEntity.ok(ApiResponse.ok(branchService.getActiveBranches()));
    }

    @GetMapping("/public/branches/{id}")
    public ResponseEntity<ApiResponse<BranchDto.BranchResponse>> getBranch(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(branchService.getBranch(id)));
    }

    @PutMapping("/manager/branches/{branchId}/products/{productId}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> updateProductAvailability(
            @PathVariable("branchId") Long branchId,
            @PathVariable("productId") Long productId,
            @Valid @RequestBody BranchDto.UpdateAvailabilityRequest request,
            @AuthenticationPrincipal CurrentActor actor) {
        branchService.updateProductAvailability(branchId, productId, request.getAvailable(), actor);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/admin/branches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BranchDto.BranchResponse>> createBranch(
            @Valid @RequestBody BranchDto.CreateBranchRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(branchService.createBranch(request)));
    }

    @PutMapping("/admin/branches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BranchDto.BranchResponse>> updateBranch(
            @PathVariable("id") Long id,
            @Valid @RequestBody BranchDto.UpdateBranchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(branchService.updateBranch(id, request)));
    }

    @PostMapping("/admin/branches/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BranchDto.BranchResponse>> toggleBranchStatus(
            @PathVariable("id") Long id,
            @RequestBody(required = false) java.util.Map<String, Boolean> body) {
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(ApiResponse.ok(branchService.toggleBranchStatus(id, active)));
    }
}
