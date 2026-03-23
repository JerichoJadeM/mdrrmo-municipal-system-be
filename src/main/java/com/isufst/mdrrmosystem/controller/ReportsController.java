package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.*;
import com.isufst.mdrrmosystem.service.ReportsExportService;
import com.isufst.mdrrmosystem.service.ReportsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ReportsExportService reportsExportService;

    public ReportsController(ReportsService reportsService, ReportsExportService reportsExportService) {
        this.reportsService = reportsService;
        this.reportsExportService = reportsExportService;
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

    @GetMapping("/incidents")
    public IncidentReportResponse getIncidentReport(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return reportsService.getIncidentReport(from, to);
    }

    @GetMapping("/calamities")
    public CalamityReportResponse getCalamityReport(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return reportsService.getCalamityReport(from, to);
    }

    @GetMapping("/resources")
    public ResourceReportResponse getResourceReport(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer year
    ) {
        return reportsService.getResourceReport(from, to, year);
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam String tab,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer year
    ) {
        return reportsExportService.exportPdf(tab, from, to, year);
    }
}
