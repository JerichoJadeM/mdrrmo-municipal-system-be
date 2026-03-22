package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.AuditTrailResponse;
import com.isufst.mdrrmosystem.response.FinancialReportResponse;
import com.isufst.mdrrmosystem.response.ReportsSummaryResponse;
import com.isufst.mdrrmosystem.service.ReportsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/summary")
    public ReportsSummaryResponse getSummary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer year
    ) {
        return reportsService.getSummary(from, to, year);
    }

    @GetMapping("/financial")
    public FinancialReportResponse getFinancial(
            @RequestParam(required = false) Integer year
    ) {
        return reportsService.getFinancial(year);
    }

    @GetMapping("/audit-trail")
    public List<AuditTrailResponse> getAuditTrail(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long operationId
    ) {
        return reportsService.getAuditTrail(operationType, actionType, performedBy, from, to, operationId);
    }
}
