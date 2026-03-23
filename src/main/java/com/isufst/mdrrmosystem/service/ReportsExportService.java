package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.response.*;
import com.isufst.mdrrmosystem.response.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportsExportService {

    private final ReportsService reportsService;

    public ReportsExportService(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    public ResponseEntity<byte[]> exportPdf(String tab, LocalDate from, LocalDate to, Integer year) {
        byte[] pdf = generatePdf(tab, from, to, year);

        String fileName = "report-" + tab + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private byte[] generatePdf(String tab, LocalDate from, LocalDate to, Integer year) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("MDRRMO Reports", titleFont));
            document.add(new Paragraph("Tab: " + tab, bodyFont));
            document.add(new Paragraph("Generated: " + LocalDateTime.now(), bodyFont));
            document.add(new Paragraph(" "));

            switch (tab.toLowerCase()) {
                case "summary" -> writeSummary(document, sectionFont, bodyFont, from, to, year);
                case "financial" -> writeFinancial(document, sectionFont, bodyFont, year);
                case "audit" -> writeAudit(document, sectionFont, bodyFont, from, to);
                case "incidents" -> writeIncidents(document, sectionFont, bodyFont, from, to);
                case "calamities" -> writeCalamities(document, sectionFont, bodyFont, from, to);
                case "resources" -> writeResources(document, sectionFont, bodyFont, from, to, year);
                default -> document.add(new Paragraph("Unsupported tab: " + tab, bodyFont));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void writeSummary(Document document, Font sectionFont, Font bodyFont,
                              LocalDate from, LocalDate to, Integer year) throws DocumentException {
        ReportsSummaryResponse summary = reportsService.getSummary(from, to, year);

        document.add(new Paragraph("Operational Summary", sectionFont));
        document.add(new Paragraph("Total Incidents: " + summary.totalIncidents(), bodyFont));
        document.add(new Paragraph("Active Incidents: " + summary.activeIncidents(), bodyFont));
        document.add(new Paragraph("Resolved Incidents: " + summary.resolvedIncidents(), bodyFont));
        document.add(new Paragraph("Total Calamities: " + summary.totalCalamities(), bodyFont));
        document.add(new Paragraph("Active Calamities: " + summary.activeCalamities(), bodyFont));
        document.add(new Paragraph("Resolved Calamities: " + summary.resolvedCalamities(), bodyFont));
        document.add(new Paragraph("Inventory Items: " + summary.totalInventoryItems(), bodyFont));
        document.add(new Paragraph("Low Stock Items: " + summary.lowStockItems(), bodyFont));
        document.add(new Paragraph("Open Evacuation Centers: " + summary.openEvacuationCenters(), bodyFont));
        document.add(new Paragraph("Audit Events: " + summary.totalAuditEvents(), bodyFont));
        document.add(new Paragraph("Budget: " + summary.currentYearBudget(), bodyFont));
        document.add(new Paragraph("Spent: " + summary.currentYearSpent(), bodyFont));
        document.add(new Paragraph("Remaining: " + summary.currentYearRemaining(), bodyFont));
    }

    private void writeFinancial(Document document, Font sectionFont, Font bodyFont,
                                Integer year) throws DocumentException {
        FinancialReportResponse financial = reportsService.getFinancial(year);

        document.add(new Paragraph("Financial Report", sectionFont));
        document.add(new Paragraph("Year: " + financial.year(), bodyFont));
        document.add(new Paragraph("Total Allotment: " + financial.currentSummary().totalAllotment(), bodyFont));
        document.add(new Paragraph("Total Allocated: " + financial.currentSummary().totalAllocated(), bodyFont));
        document.add(new Paragraph("Total Obligations: " + financial.currentSummary().totalObligations(), bodyFont));
        document.add(new Paragraph("Remaining: " + financial.currentSummary().totalRemaining(), bodyFont));
        document.add(new Paragraph("Allocation Rate: " + financial.currentSummary().allocationRate(), bodyFont));
        document.add(new Paragraph("Utilization Rate: " + financial.currentSummary().utilizationRate(), bodyFont));
        document.add(new Paragraph("Forecast Year: " + financial.nextYearForecast().year(), bodyFont));
        document.add(new Paragraph("Projected Budget: " + financial.nextYearForecast().totalForecast(), bodyFont));
        document.add(new Paragraph("Assumptions: " + financial.nextYearForecast().assumptions(), bodyFont));
    }

    private void writeAudit(Document document, Font sectionFont, Font bodyFont,
                            LocalDate from, LocalDate to) throws DocumentException {
        java.util.List<AuditTrailResponse> rows = reportsService.getAuditTrail(null, null, null, from, to, null);

        document.add(new Paragraph("Audit Trail", sectionFont));
        for (AuditTrailResponse row : rows) {
            document.add(new Paragraph(
                    row.performedAt() + " | " + row.recordType() + " | " + row.actionType() + " | " + row.performedBy(),
                    bodyFont
            ));
        }
    }

    private void writeIncidents(Document document, Font sectionFont, Font bodyFont,
                                LocalDate from, LocalDate to) throws DocumentException {
        IncidentReportResponse report = reportsService.getIncidentReport(from, to);

        document.add(new Paragraph("Incident Report", sectionFont));
        document.add(new Paragraph("Total: " + report.totalIncidents(), bodyFont));
        document.add(new Paragraph("Active: " + report.activeIncidents(), bodyFont));
        document.add(new Paragraph("Resolved: " + report.resolvedIncidents(), bodyFont));
    }

    private void writeCalamities(Document document, Font sectionFont, Font bodyFont,
                                 LocalDate from, LocalDate to) throws DocumentException {
        CalamityReportResponse report = reportsService.getCalamityReport(from, to);

        document.add(new Paragraph("Calamity Report", sectionFont));
        document.add(new Paragraph("Total: " + report.totalCalamities(), bodyFont));
        document.add(new Paragraph("Active: " + report.activeCalamities(), bodyFont));
        document.add(new Paragraph("Monitoring: " + report.monitoringCalamities(), bodyFont));
        document.add(new Paragraph("Resolved: " + report.resolvedCalamities(), bodyFont));
        document.add(new Paragraph("Ended: " + report.endedCalamities(), bodyFont));
    }

    private void writeResources(Document document, Font sectionFont, Font bodyFont,
                                LocalDate from, LocalDate to, Integer year) throws DocumentException {
        ResourceReportResponse report = reportsService.getResourceReport(from, to, year);

        document.add(new Paragraph("Resources Report", sectionFont));
        document.add(new Paragraph("Inventory Count: " + report.inventoryCount(), bodyFont));
        document.add(new Paragraph("Low Stock Count: " + report.lowStockCount(), bodyFont));
        document.add(new Paragraph("Evacuation Centers: " + report.evacuationCenterCount(), bodyFont));
        document.add(new Paragraph("Open Evacuation Centers: " + report.openEvacuationCenters(), bodyFont));
        document.add(new Paragraph("Relief Distributions: " + report.reliefDistributionCount(), bodyFont));
    }
}
