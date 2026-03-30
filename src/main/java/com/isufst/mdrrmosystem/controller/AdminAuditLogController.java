package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.AuditTrailResponse;
import com.isufst.mdrrmosystem.service.ReportsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final ReportsService reportsService;

    public AdminAuditLogController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping
    public List<AuditTrailResponse> getAdminAuditLogs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return reportsService.getAuditTrail(
                "ADMINISTRATION",
                category,
                actionType,
                performedBy,
                from,
                to,
                null
        );
    }
}
