package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.response.AuditTrailResponse;
import com.isufst.mdrrmosystem.response.BudgetCurrentSummaryResponse;
import com.isufst.mdrrmosystem.response.FinancialReportResponse;
import com.isufst.mdrrmosystem.response.ReportsSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportsService {

    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final InventoryRepository inventoryRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final OperationHistoryRepository operationHistoryRepository;
    private final BudgetService budgetService;
    private final BudgetAnalyticsService budgetAnalyticsService;
    private final BudgetForecastService budgetForecastService;

    public ReportsService(IncidentRepository incidentRepository,
                          CalamityRepository calamityRepository,
                          InventoryRepository inventoryRepository,
                          EvacuationActivationRepository evacuationActivationRepository,
                          OperationHistoryRepository operationHistoryRepository,
                          BudgetService budgetService,
                          BudgetAnalyticsService budgetAnalyticsService,
                          BudgetForecastService budgetForecastService) {
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.inventoryRepository = inventoryRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.operationHistoryRepository = operationHistoryRepository;
        this.budgetService = budgetService;
        this.budgetAnalyticsService = budgetAnalyticsService;
        this.budgetForecastService = budgetForecastService;
    }

    public ReportsSummaryResponse getSummary(LocalDate from, LocalDate to, Integer year) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

        long totalIncidents = incidentRepository.countAllWithin(fromDateTime, toDateTime);
        long activeIncidents =
                incidentRepository.countByStatusWithin("ONGOING", fromDateTime, toDateTime)
                        + incidentRepository.countByStatusWithin("IN_PROGRESS", fromDateTime, toDateTime)
                        + incidentRepository.countByStatusWithin("ON_SITE", fromDateTime, toDateTime);

        long resolvedIncidents = incidentRepository.countByStatusWithin("RESOLVED", fromDateTime, toDateTime);

        long totalCalamities = calamityRepository.countAllWithin(from, to);
        long activeCalamities =
                calamityRepository.countByStatusWithin("ACTIVE", from, to)
                        + calamityRepository.countByStatusWithin("MONITORING", from, to);

        long resolvedCalamities =
                calamityRepository.countByStatusWithin("RESOLVED", from, to)
                        + calamityRepository.countByStatusWithin("ENDED", from, to);

        List<Inventory> inventoryItems = inventoryRepository.findAll();
        long totalInventoryItems = inventoryItems.size();
        long lowStockItems = inventoryItems.stream().filter(this::isLowStock).count();

        long openEvacuationCenters = evacuationActivationRepository.countByStatus("OPEN");

        long totalAuditEvents = operationHistoryRepository.countAuditTrail(
                null, null, null, null, fromDateTime, toDateTime
        );

        int resolvedYear;
        if (year != null) {
            resolvedYear = year;
        } else if (to != null) {
            resolvedYear = to.getYear();
        } else {
            resolvedYear = LocalDate.now().getYear();
        }

        BudgetCurrentSummaryResponse currentSummary = budgetService.getSummaryByYear(resolvedYear);

        return new ReportsSummaryResponse(
                totalIncidents,
                activeIncidents,
                resolvedIncidents,
                totalCalamities,
                activeCalamities,
                resolvedCalamities,
                totalInventoryItems,
                lowStockItems,
                openEvacuationCenters,
                totalAuditEvents,
                currentSummary.totalAllotment(),
                currentSummary.totalObligations(),
                currentSummary.totalRemaining()
        );
    }

    public FinancialReportResponse getFinancial(Integer year) {
        int resolvedYear = year != null ? year : LocalDate.now().getYear();

        return new FinancialReportResponse(
                resolvedYear,
                budgetService.getSummaryByYear(resolvedYear),
                budgetService.getBudgetHistory(5),
                budgetForecastService.getNextYearForecast(),
                budgetAnalyticsService.getBudgetAnalytics(resolvedYear)
        );
    }

    public List<AuditTrailResponse> getAuditTrail(String operationType,
                                                  String actionType,
                                                  String performedBy,
                                                  LocalDate from,
                                                  LocalDate to,
                                                  Long operationId) {

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

        return operationHistoryRepository.searchAuditTrail(
                        normalize(operationType),
                        normalize(actionType),
                        normalize(performedBy),
                        operationId,
                        fromDateTime,
                        toDateTime
                )
                .stream()
                .map(history -> new AuditTrailResponse(
                        history.getId(),
                        "OPERATIONS",
                        history.getOperationType(),
                        history.getOperationId(),
                        history.getActionType(),
                        history.getFromStatus(),
                        history.getToStatus(),
                        history.getDescription(),
                        history.getMetadataJson(),
                        history.getPerformedBy(),
                        history.getPerformedAt()
                ))
                .toList();
    }

    private boolean isLowStock(Inventory inventory) {
        if (inventory.getAvailableQuantity() <= 0) {
            return true;
        }
        int reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0;
        return inventory.getAvailableQuantity() <= reorderLevel;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String operationType(String value) {
        return value;
    }

    private String actionType(String value) {
        return value;
    }

    private String performedBy(String value) {
        return value;
    }
}
