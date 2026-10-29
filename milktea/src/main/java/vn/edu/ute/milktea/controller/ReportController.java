package vn.edu.ute.milktea.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.ReportDto;
import vn.edu.ute.milktea.service.report.ReportService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import vn.edu.ute.milktea.entity.account.Role;
import vn.edu.ute.milktea.security.CurrentActor;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<ReportDto.RevenueReportView>> getRevenueReport(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CurrentActor actor) {

        Long targetBranchId = resolveBranchId(branchId, actor);
        return ResponseEntity.ok(ApiResponse.ok(reportService.getRevenueReport(targetBranchId, fromDate, toDate)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<ReportDto.OrderReportView>> getOrderReport(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal CurrentActor actor) {

        Long targetBranchId = resolveBranchId(branchId, actor);
        return ResponseEntity.ok(ApiResponse.ok(reportService.getOrderReport(targetBranchId, fromDate, toDate)));
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ReportDto.DashboardResponse>> getDashboard(
            @RequestParam(value = "branchId", required = false) Long branchId,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @AuthenticationPrincipal CurrentActor actor) {

        Long targetBranchId = resolveBranchId(branchId, actor);
        var dashboard = reportService.getDashboard(targetBranchId, fromDate, toDate);
        // Do not expose cross-branch rankings to Managers, even without an assignment.
        dashboard.setBranchRevenueRanking(
                actor != null && actor.hasRole(Role.ADMIN) && targetBranchId == null
                        ? reportService.getBranchRevenueRanking(
                                dashboard.getFromDate(), dashboard.getToDate())
                        : java.util.List.of());
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    private Long resolveBranchId(Long branchId, CurrentActor actor) {
        if (actor != null && actor.hasRole(Role.MANAGER)) {
            return actor.getBranchId();
        }
        return branchId;
    }

}
