package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.response.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ReportsService {

    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final InventoryRepository inventoryRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final OperationHistoryRepository operationHistoryRepository;
    private final BudgetService budgetService;
    private final BudgetAnalyticsService budgetAnalyticsService;
    private final BudgetForecastService budgetForecastService;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final EvacuationCenterRepository evacuationCenterRepository;

    public ReportsService(IncidentRepository incidentRepository,
                          CalamityRepository calamityRepository,
                          InventoryRepository inventoryRepository,
                          EvacuationActivationRepository evacuationActivationRepository,
                          OperationHistoryRepository operationHistoryRepository,
                          BudgetService budgetService,
                          BudgetAnalyticsService budgetAnalyticsService,
                          BudgetForecastService budgetForecastService,
                          InventoryTransactionRepository inventoryTransactionRepository,
                          ReliefDistributionRepository reliefDistributionRepository,
                          EvacuationCenterRepository evacuationCenterRepository) {
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.inventoryRepository = inventoryRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.operationHistoryRepository = operationHistoryRepository;
        this.budgetService = budgetService;
        this.budgetAnalyticsService = budgetAnalyticsService;
        this.budgetForecastService = budgetForecastService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.evacuationCenterRepository = evacuationCenterRepository;
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

    public IncidentReportResponse getIncidentReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<Incident> incidents = incidentRepository.findAllWithin(fromDateTime, toDateTime);

        long totalIncidents = incidents.size();
        long activeIncidents = incidents.stream()
                .filter(i -> List.of("ONGOING", "IN_PROGRESS", "ON_SITE").contains(i.getStatus()))
                .count();
        long resolvedIncidents = incidents.stream()
                .filter(i -> "RESOLVED".equalsIgnoreCase(i.getStatus()))
                .count();

        List<CountByLabelResponse> byType = incidents.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getType() != null ? i.getType() : "Unspecified",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new CountByLabelResponse(String.valueOf(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(CountByLabelResponse::label))
                .toList();

        List<CountByLabelResponse> byBarangay = incidents.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getBarangay() != null ? i.getBarangay().getName() : "Unspecified",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new CountByLabelResponse(String.valueOf(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(CountByLabelResponse::label))
                .toList();

        List<IncidentReportItemResponse> rows = incidents.stream()
                .sorted(Comparator.comparing(Incident::getReportedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(i -> new IncidentReportItemResponse(
                        i.getId(),
                        i.getType(),
                        i.getStatus(),
                        i.getBarangay().getName(),
                        i.getBarangay().getName(),
                        i.getReportedAt()
                ))
                .toList();

        return new IncidentReportResponse(
                totalIncidents,
                activeIncidents,
                resolvedIncidents,
                byType,
                byBarangay,
                rows
        );
    }

    public CalamityReportResponse getCalamityReport(LocalDate from, LocalDate to) {
        List<Calamity> calamities = calamityRepository.findAllWithinRange(from, to);

        long totalCalamities = calamities.size();
        long activeCalamities = calamities.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus())).count();
        long monitoringCalamities = calamities.stream().filter(c -> "MONITORING".equalsIgnoreCase(c.getStatus())).count();
        long resolvedCalamities = calamities.stream().filter(c -> "RESOLVED".equalsIgnoreCase(c.getStatus())).count();
        long endedCalamities = calamities.stream().filter(c -> "ENDED".equalsIgnoreCase(c.getStatus())).count();

        List<CountByLabelResponse> byType = calamities.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getType() != null ? c.getType() : "Unspecified",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new CountByLabelResponse(String.valueOf(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(CountByLabelResponse::label))
                .toList();

        List<CountByLabelResponse> byBarangay = calamities.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getBarangay() != null ? c.getBarangay().getName() : "Unspecified",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new CountByLabelResponse(String.valueOf(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(CountByLabelResponse::label))
                .toList();

        List<CalamityReportItemResponse> rows = calamities.stream()
                .sorted(Comparator.comparing(Calamity::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> new CalamityReportItemResponse(
                        c.getId(),
                        c.getType(),
                        c.getStatus(),
                        c.getAffectedAreaTypes() != null ? c.getAffectedAreaTypes() : "--",
                        c.getBarangay() != null ? c.getBarangay().getName() : "--",
                        c.getDate()
                ))
                .toList();

        return new CalamityReportResponse(
                totalCalamities,
                activeCalamities,
                monitoringCalamities,
                resolvedCalamities,
                endedCalamities,
                byType,
                byBarangay,
                rows
        );
    }

    public ResourceReportResponse getResourceReport(LocalDate from, LocalDate to, Integer year) {
        List<Inventory> inventory = inventoryRepository.findAll();
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findAllWithin(
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null
        );
        List<ReliefDistribution> distributions = reliefDistributionRepository.findAllWithinRange(
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null
        );

        long inventoryCount = inventory.size();
        long lowStockCount = inventory.stream().filter(this::isLowStock).count();
        long evacuationCenterCount = evacuationCenterRepository.count();
        long openEvacuationCenters = evacuationActivationRepository.countByStatus("OPEN");
        long reliefDistributionCount = distributions.size();

        List<InventoryReportItemResponse> lowStockItems = inventory.stream()
                .filter(this::isLowStock)
                .map(item -> new InventoryReportItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getCategory(),
                        item.getAvailableQuantity(),
                        item.getReorderLevel(),
                        resolveStockStatus(item)
                ))
                .toList();

        List<InventoryTransactionReportResponse> inventoryTransactions = transactions.stream()
                .map(tx -> new InventoryTransactionReportResponse(
                        tx.getId(),
                        tx.getInventory() != null ? tx.getInventory().getId() : null,
                        tx.getInventory() != null ? tx.getInventory().getName() : "--",
                        tx.getActionType(),
                        tx.getQuantity(),
                        tx.getPerformedBy() != null ? tx.getPerformedBy().getFirstName() + " " + tx.getPerformedBy().getLastName() : "--",
                        tx.getTimeStamp()
                ))
                .toList();

        List<ReliefDistributionReportItemResponse> reliefRows = distributions.stream()
                .sorted(Comparator.comparing(ReliefDistribution::getDistributedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(d -> {
                    String referenceType = d.getIncident() != null ? "INCIDENT"
                            : d.getCalamity() != null ? "CALAMITY"
                            : d.getEvacuationActivation() != null ? "EVACUATION"
                            : "--";

                    Long referenceId = d.getIncident() != null ? d.getIncident().getId()
                            : d.getCalamity() != null ? d.getCalamity().getId()
                            : d.getEvacuationActivation() != null ? d.getEvacuationActivation().getId()
                            : null;

                    String itemName = d.getInventory() != null ? d.getInventory().getName() : "--";

                    String distributedBy = d.getDistributedBy() != null
                            ? (d.getDistributedBy().getFirstName() + " " + d.getDistributedBy().getLastName()).trim()
                            : "--";

                    return new ReliefDistributionReportItemResponse(
                            d.getId(),
                            referenceType,
                            referenceId,
                            itemName,
                            d.getQuantity(),
                            distributedBy,
                            d.getDistributedAt()
                    );
                })
                .toList();

        return new ResourceReportResponse(
                inventoryCount,
                lowStockCount,
                evacuationCenterCount,
                openEvacuationCenters,
                reliefDistributionCount,
                lowStockItems,
                inventoryTransactions,
                reliefRows
        );
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

    private String resolveStockStatus(Inventory item) {
        int available = item.getAvailableQuantity();
        Integer reorderLevel = item.getReorderLevel();

        if (available <= 0) {
            return "OUT OF STOCK";
        }

        if (reorderLevel != null && available <= reorderLevel) {
            return "LOW STOCK";
        }

        return "AVAILABLE";
    }
}
