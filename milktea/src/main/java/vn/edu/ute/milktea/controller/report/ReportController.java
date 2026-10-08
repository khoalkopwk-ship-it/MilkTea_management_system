package vn.edu.ute.milktea.controller.report;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ute.milktea.common.ApiResponse;
import vn.edu.ute.milktea.dto.report.ReportDto;
import vn.edu.ute.milktea.service.report.ReportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<ReportDto.SalesReportResponse>> getSalesReport(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.getSalesReport(startDate, endDate)));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<ReportDto.RevenueReportView>> getRevenueReport(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.getRevenueReport(fromDate, toDate)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<ReportDto.OrderReportView>> getOrderReport(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.getOrderReport(fromDate, toDate)));
    }
}
