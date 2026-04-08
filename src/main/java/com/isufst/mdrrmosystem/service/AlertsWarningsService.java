package com.isufst.mdrrmosystem.service;


import org.springframework.stereotype.Service;
import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.OperationHistory;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.OperationHistoryRepository;
import com.isufst.mdrrmosystem.repository.UserRepository;
import com.isufst.mdrrmosystem.response.ActiveAlertResponse;
import com.isufst.mdrrmosystem.response.AlertsMetricResponse;
import com.isufst.mdrrmosystem.response.AlertsReadinessDomainResponse;
import com.isufst.mdrrmosystem.response.AlertsWarningsOverviewResponse;
import com.isufst.mdrrmosystem.response.AlertsWarningsSummaryResponse;
import com.isufst.mdrrmosystem.response.BudgetCurrentSummaryResponse;
import com.isufst.mdrrmosystem.response.MunicipalWeatherForecastResponse;
import com.isufst.mdrrmosystem.response.PriorityActionResponse;
import com.isufst.mdrrmosystem.response.ReadinessNoteResponse;
import com.isufst.mdrrmosystem.response.ResourcesReadinessSummaryResponse;
import com.isufst.mdrrmosystem.response.WarningHistoryResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AlertsWarningsService {

    private static final Set<String> RELIEF_CATEGORIES =
            Set.of("FOOD", "RELIEF", "WATER", "HYGIENE", "MEDICAL");

    private final WeatherForecastService weatherForecastService;
    private final ResourcesReadinessService resourcesReadinessService;
    private final BudgetService budgetService;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final InventoryRepository inventoryRepository;
    private final OperationHistoryRepository operationHistoryRepository;

    public AlertsWarningsService(WeatherForecastService weatherForecastService,
                                 ResourcesReadinessService resourcesReadinessService,
                                 BudgetService budgetService,
                                 UserRepository userRepository,
                                 IncidentRepository incidentRepository,
                                 CalamityRepository calamityRepository,
                                 EvacuationActivationRepository evacuationActivationRepository,
                                 InventoryRepository inventoryRepository,
                                 OperationHistoryRepository operationHistoryRepository) {
        this.weatherForecastService = weatherForecastService;
        this.resourcesReadinessService = resourcesReadinessService;
        this.budgetService = budgetService;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.inventoryRepository = inventoryRepository;
        this.operationHistoryRepository = operationHistoryRepository;
    }

    @Transactional(readOnly = true)
    public AlertsWarningsOverviewResponse getOverview() {

        // 🔹 External + summaries (OK)
        var weather = weatherForecastService.getMunicipalForecast();
        var resources = resourcesReadinessService.getReadinessSummary();
        var budget = budgetService.getCurrentSummary();

        // 🔹 COUNT instead of LOAD
        long availableResponders = userRepository.countAvailableResponders();

        long activeIncidents =
                incidentRepository.countByStatus("ONGOING") +
                        incidentRepository.countByStatus("IN_PROGRESS") +
                        incidentRepository.countByStatus("ON_SITE");

        long activeCalamities =
                calamityRepository.countByStatusIn(List.of("ACTIVE", "MONITORING"));

        long criticalOutOfStock = inventoryRepository.countCriticalOutOfStock();
        long criticalLowStock = inventoryRepository.countCriticalLowStock();

        long openCenters =
                evacuationActivationRepository.countByStatus("OPEN");

        // 🔹 LIMITED history
        List<OperationHistory> history =
                operationHistoryRepository.findTop6ByOrderByPerformedAtDesc();

        // 🔥 Build lightweight alerts (NO heavy loops)
        List<ActiveAlertResponse> alerts = new ArrayList<>();

        if (criticalOutOfStock > 0) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    "CRITICAL",
                    "ACTIVE",
                    "Critical inventory is out of stock",
                    "Immediate replenishment required",
                    "Inventory",
                    now(),
                    "System",
                    "Restock immediately"
            ));
        } else if (criticalLowStock > 0) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    "HIGH",
                    "ACTIVE",
                    "Critical inventory running low",
                    "Monitor and replenish soon",
                    "Inventory",
                    now(),
                    "System",
                    "Prepare restock"
            ));
        }

        if (availableResponders < 4) {
            alerts.add(new ActiveAlertResponse(
                    "PERSONNEL",
                    "CRITICAL",
                    "ACTIVE",
                    "Low responder availability",
                    "Insufficient responders for operations",
                    "Personnel",
                    now(),
                    "System",
                    "Activate reserves"
            ));
        }

        if (budget.utilizationRate() >= 90) {
            alerts.add(new ActiveAlertResponse(
                    "BUDGET",
                    "CRITICAL",
                    "ACTIVE",
                    "Budget critical level",
                    "Budget almost depleted",
                    "Budget",
                    now(),
                    "System",
                    "Review expenses"
            ));
        }

        // 🔹 Map history (small only)
        List<WarningHistoryResponse> historyResponse = history.stream()
                .map(h -> new WarningHistoryResponse(
                        safe(h.getActionType()),
                        safe(h.getDescription()),
                        safe(h.getToStatus()),
                        h.getPerformedAt() != null ? h.getPerformedAt().toString() : null
                ))
                .toList();

        // 🔹 Summary
        AlertsWarningsSummaryResponse summary = new AlertsWarningsSummaryResponse(
                mapOverall(resources.overallReadinessRiskLevel()),
                alerts.size(),
                (int) alerts.stream().filter(a -> isCritical(a.severity())).count(),
                availableResponders + " Available"
        );

        return new AlertsWarningsOverviewResponse(
                now(),
                summary,
                List.of(), // keep minimal or rebuild lightweight
                alerts,
                List.of(), // optional
                historyResponse,
                List.of()  // optional
        );
    }

    private String now() {
        return LocalDateTime.now().toString();
    }

    private boolean isCritical(String s) {
        return "CRITICAL".equalsIgnoreCase(s) || "HIGH".equalsIgnoreCase(s);
    }

    private String safe(String v) {
        return v == null || v.isBlank() ? "--" : v;
    }

    private String mapOverall(String risk) {
        return switch (risk == null ? "" : risk.toUpperCase()) {
            case "LOW" -> "Ready";
            case "MEDIUM", "HIGH" -> "Partially Ready";
            default -> "Not Ready";
        };
    }

    private List<AlertsReadinessDomainResponse> buildReadinessDomains(
            MunicipalWeatherForecastResponse weather,
            ResourcesReadinessSummaryResponse resources,
            BudgetCurrentSummaryResponse budget,
            List<User> availableResponders,
            List<EvacuationActivation> openCenters,
            long activeIncidents,
            long activeCalamities,
            List<Inventory> inventory
    ) {
        List<AlertsReadinessDomainResponse> rows = new ArrayList<>();

        rows.add(buildWeatherDomain(weather));
        rows.add(buildPersonnelDomain(availableResponders, activeIncidents, activeCalamities));
        rows.add(buildResourceDomain(resources, inventory));
        rows.add(buildEvacuationDomain(resources, openCenters));
        rows.add(buildBudgetDomain(resources, budget));

        return rows;
    }

    private AlertsReadinessDomainResponse buildWeatherDomain(MunicipalWeatherForecastResponse weather) {
        String weatherRisk = weather.summary() != null ? weather.summary().overallRiskLevel() : "MEDIUM";
        String readinessStatus = mapRiskToReadiness(weatherRisk);

        int highRiskBarangays = weather.summary() != null ? weather.summary().highRiskBarangays() : 0;
        int mediumRiskBarangays = weather.summary() != null ? weather.summary().mediumRiskBarangays() : 0;
        int totalBarangays = weather.summary() != null ? weather.summary().totalBarangays() : 0;

        int score = switch (normalize(weatherRisk)) {
            case "LOW" -> 88;
            case "MEDIUM" -> 64;
            case "HIGH" -> 42;
            case "CRITICAL", "SEVERE" -> 25;
            default -> 60;
        };

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Overall Risk", safe(weatherRisk)),
                new AlertsMetricResponse("Rainfall Outlook", weather.summary() != null ? safe(weather.summary().rainfallOutlook()) : "--"),
                new AlertsMetricResponse("High Risk Barangays", String.valueOf(highRiskBarangays)),
                new AlertsMetricResponse("Monitored Barangays", totalBarangays > 0 ? String.valueOf(totalBarangays) : "--")
        );

        String note = weather.summary() != null
                ? safe(weather.summary().recommendation())
                : "Continue municipal weather monitoring.";

        return new AlertsReadinessDomainResponse(
                "WEATHER",
                "Weather Readiness",
                "Forecast-driven early warning posture for Batad, Iloilo.",
                readinessStatus,
                score,
                note,
                metrics
        );
    }

    private AlertsReadinessDomainResponse buildPersonnelDomain(List<User> availableResponders,
                                                               long activeIncidents,
                                                               long activeCalamities) {
        int available = availableResponders.size();
        String status;
        int score;

        if (available >= 8) {
            status = "READY";
            score = 85;
        } else if (available >= 4) {
            status = "LIMITED";
            score = 62;
        } else {
            status = "CRITICAL";
            score = 30;
        }

        String note;
        if (available < 4) {
            note = "Responder reserve is critically low for simultaneous field operations.";
        } else if (activeIncidents + activeCalamities >= 3) {
            note = "Current operations are active. Maintain reserve personnel monitoring.";
        } else {
            note = "Current responder availability supports routine response readiness.";
        }

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Available Responders", String.valueOf(available)),
                new AlertsMetricResponse("Active Incidents", String.valueOf(activeIncidents)),
                new AlertsMetricResponse("Active Calamities", String.valueOf(activeCalamities)),
                new AlertsMetricResponse("Responder Posture", status)
        );

        return new AlertsReadinessDomainResponse(
                "PERSONNEL",
                "Personnel Readiness",
                "Availability of responders for deployment and sustained field operations.",
                status,
                score,
                note,
                metrics
        );
    }

    private AlertsReadinessDomainResponse buildResourceDomain(ResourcesReadinessSummaryResponse resources,
                                                              List<Inventory> inventory) {
        long criticalItemsAtRisk = inventory.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(this::isLowOrOutOfStock)
                .count();

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Inventory Risk", safe(resources.inventoryRiskLevel())),
                new AlertsMetricResponse("Low Stock Items", String.valueOf(resources.inventoryLowStockCount())),
                new AlertsMetricResponse("Out of Stock", String.valueOf(resources.inventoryOutOfStockCount())),
                new AlertsMetricResponse("Critical Items At Risk", String.valueOf(criticalItemsAtRisk))
        );

        String note = resources.inventoryOutOfStockCount() > 0
                ? "One or more inventory items are out of stock and may affect immediate response."
                : "Inventory remains operational, but low stock items should be monitored.";

        return new AlertsReadinessDomainResponse(
                "RESOURCE",
                "Equipment & Supply Readiness",
                "Inventory condition, rescue support items, and emergency response stock availability.",
                mapRiskToReadiness(resources.inventoryRiskLevel()),
                domainScoreFromRisk(resources.inventoryRiskLevel()),
                note,
                metrics
        );
    }

    private AlertsReadinessDomainResponse buildEvacuationDomain(ResourcesReadinessSummaryResponse resources,
                                                                List<EvacuationActivation> openCenters) {
        int availableSlots = openCenters.stream()
                .filter(a -> a.getCenter() != null)
                .mapToInt(a -> Math.max(a.getCenter().getCapacity() - a.getCurrentEvacuees(), 0))
                .sum();

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Evacuation Risk", safe(resources.evacuationRiskLevel())),
                new AlertsMetricResponse("Open Centers", String.valueOf(resources.activeCentersCount())),
                new AlertsMetricResponse("Near Full Centers", String.valueOf(resources.nearFullCentersCount())),
                new AlertsMetricResponse("Available Slots", String.valueOf(availableSlots))
        );

        String note;
        if (resources.fullCentersCount() > 0) {
            note = "One or more open evacuation centers are already full.";
        } else if (resources.nearFullCentersCount() > 0) {
            note = "Open center capacity is tightening and may require alternate activation.";
        } else {
            note = "Open center capacity remains manageable under current evacuation load.";
        }

        return new AlertsReadinessDomainResponse(
                "EVACUATION",
                "Evacuation Readiness",
                "Current center capacity, occupancy pressure, and displacement support readiness.",
                mapRiskToReadiness(resources.evacuationRiskLevel()),
                domainScoreFromRisk(resources.evacuationRiskLevel()),
                note,
                metrics
        );
    }

    private AlertsReadinessDomainResponse buildBudgetDomain(ResourcesReadinessSummaryResponse resources,
                                                            BudgetCurrentSummaryResponse budget) {
        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Budget Risk", safe(resources.budgetRiskLevel())),
                new AlertsMetricResponse("Utilization Rate", String.format(Locale.US, "%.0f%%", budget.utilizationRate())),
                new AlertsMetricResponse("Remaining Budget", String.format(Locale.US, "₱ %,.2f", budget.totalRemaining())),
                new AlertsMetricResponse("Current Year", String.valueOf(budget.year()))
        );

        String note;
        if (budget.utilizationRate() >= 90) {
            note = "Budget utilization is critical and may constrain extended response operations.";
        } else if (budget.utilizationRate() >= 75) {
            note = "Budget utilization is elevated and should be monitored closely.";
        } else {
            note = "Current budget remains usable for ongoing operational support.";
        }

        return new AlertsReadinessDomainResponse(
                "BUDGET",
                "Budget Readiness",
                "Financial flexibility available for current and near-term operations.",
                mapRiskToReadiness(resources.budgetRiskLevel()),
                domainScoreFromRisk(resources.budgetRiskLevel()),
                note,
                metrics
        );
    }

    private List<ActiveAlertResponse> buildActiveAlerts(MunicipalWeatherForecastResponse weather,
                                                        ResourcesReadinessSummaryResponse resources,
                                                        BudgetCurrentSummaryResponse budget,
                                                        List<User> availableResponders,
                                                        List<EvacuationActivation> openCenters,
                                                        long activeIncidents,
                                                        long activeCalamities,
                                                        List<Inventory> inventory) {
        List<ActiveAlertResponse> alerts = new ArrayList<>();

        addWeatherAlerts(alerts, weather);
        addInventoryAlerts(alerts, inventory, resources);
        addPersonnelAlerts(alerts, availableResponders, activeIncidents, activeCalamities);
        addEvacuationAlerts(alerts, resources, openCenters);
        addBudgetAlerts(alerts, budget, resources);

        alerts.sort(Comparator
                .comparingInt((ActiveAlertResponse a) -> severityRank(a.severity()))
                .thenComparing(ActiveAlertResponse::title));

        return alerts;
    }

    private void addWeatherAlerts(List<ActiveAlertResponse> alerts,
                                  MunicipalWeatherForecastResponse weather) {
        if (weather.summary() != null && isAnyStatus(weather.summary().overallRiskLevel(), "HIGH", "CRITICAL", "SEVERE")) {
            alerts.add(new ActiveAlertResponse(
                    "WEATHER",
                    "HIGH",
                    "ACTIVE",
                    "Elevated municipal weather risk detected",
                    safe(weather.summary().recommendation()),
                    "Batad, Iloilo",
                    LocalDateTime.now().toString(),
                    safe(weather.source()),
                    "Increase monitoring of vulnerable barangays"
            ));
        }

        if (weather.barangayRisks() != null) {
            long highRiskCount = weather.barangayRisks().stream()
                    .filter(r -> isAnyStatus(r.riskLevel(), "HIGH", "SEVERE"))
                    .count();

            if (highRiskCount > 0) {
                alerts.add(new ActiveAlertResponse(
                        "WEATHER",
                        highRiskCount >= 3 ? "HIGH" : "MEDIUM",
                        "WATCH",
                        "High-risk barangays require closer weather monitoring",
                        highRiskCount + " barangay(s) are currently assessed at high weather-related risk.",
                        "Selected barangays in Batad",
                        LocalDateTime.now().toString(),
                        safe(weather.source()),
                        "Prepare barangay-level readiness checks"
                ));
            }
        }
    }

    private void addInventoryAlerts(List<ActiveAlertResponse> alerts,
                                    List<Inventory> inventory,
                                    ResourcesReadinessSummaryResponse resources) {
        long criticalOutOfStock = inventory.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(i -> i.getAvailableQuantity() <= 0)
                .count();

        long criticalLowStock = inventory.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(this::isLowOrOutOfStock)
                .count();

        if (criticalOutOfStock > 0) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    "CRITICAL",
                    "ACTIVE",
                    "Critical response inventory is out of stock",
                    "One or more critical inventory items are unavailable for immediate response use.",
                    "Municipal inventory",
                    LocalDateTime.now().toString(),
                    "Resources",
                    "Restock critical response items immediately"
            ));
        } else if (criticalLowStock > 0) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    "HIGH",
                    "ACTIVE",
                    "Critical inventory stock is below preferred threshold",
                    "Critical inventory items are running low and may affect sustained operations.",
                    "Municipal inventory",
                    LocalDateTime.now().toString(),
                    "Resources",
                    "Review restocking and reserve planning"
            ));
        }

        if (resources.reliefLowStockCount() > 0 || resources.estimatedFamilyCoverage() < 50) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    resources.estimatedFamilyCoverage() < 20 ? "CRITICAL" : "MEDIUM",
                    "ACTIVE",
                    "Relief stock readiness needs replenishment",
                    "Relief-related inventory coverage is lower than preferred for emergency support.",
                    "Relief storage",
                    LocalDateTime.now().toString(),
                    "Resources",
                    "Replenish food, relief, and essential support items"
            ));
        }
    }

    private void addPersonnelAlerts(List<ActiveAlertResponse> alerts,
                                    List<User> availableResponders,
                                    long activeIncidents,
                                    long activeCalamities) {
        int available = availableResponders.size();

        if (available < 4) {
            alerts.add(new ActiveAlertResponse(
                    "PERSONNEL",
                    "CRITICAL",
                    "ACTIVE",
                    "Responder availability is critically low",
                    "Available responder count is below the preferred minimum for concurrent operations.",
                    "MDRRMO personnel pool",
                    LocalDateTime.now().toString(),
                    "Operations",
                    "Review standby roster and reserve assignments"
            ));
        } else if (available < 8) {
            alerts.add(new ActiveAlertResponse(
                    "PERSONNEL",
                    "MEDIUM",
                    "WATCH",
                    "Responder reserve capacity is limited",
                    "Current available responder count may be insufficient for multiple simultaneous incidents.",
                    "MDRRMO personnel pool",
                    LocalDateTime.now().toString(),
                    "Operations",
                    "Monitor staffing coverage for next shifts"
            ));
        }

        if ((activeIncidents + activeCalamities) >= 4) {
            alerts.add(new ActiveAlertResponse(
                    "OPERATIONAL",
                    "HIGH",
                    "ACTIVE",
                    "Operational demand is elevated",
                    "Multiple active operations may place pressure on manpower and logistics readiness.",
                    "Municipal operations",
                    LocalDateTime.now().toString(),
                    "Operations",
                    "Review deployment balance and reserve posture"
            ));
        }
    }

    private void addEvacuationAlerts(List<ActiveAlertResponse> alerts,
                                     ResourcesReadinessSummaryResponse resources,
                                     List<EvacuationActivation> openCenters) {
        if (resources.fullCentersCount() > 0) {
            alerts.add(new ActiveAlertResponse(
                    "EVACUATION",
                    "CRITICAL",
                    "ACTIVE",
                    "One or more evacuation centers are already full",
                    "Current center occupancy has reached full capacity for at least one active evacuation site.",
                    "Open evacuation centers",
                    LocalDateTime.now().toString(),
                    "Evacuation Centers",
                    "Prepare alternate center activation"
            ));
        } else if (resources.nearFullCentersCount() > 0) {
            alerts.add(new ActiveAlertResponse(
                    "EVACUATION",
                    "HIGH",
                    "WATCH",
                    "Evacuation center occupancy is rising",
                    "One or more open centers are nearing preferred occupancy limits.",
                    "Open evacuation centers",
                    LocalDateTime.now().toString(),
                    "Evacuation Centers",
                    "Review overflow capacity and alternate sites"
            ));
        }

        if (!openCenters.isEmpty() && resources.overallOccupancyRate() >= 80) {
            alerts.add(new ActiveAlertResponse(
                    "EVACUATION",
                    "MEDIUM",
                    "WATCH",
                    "Overall evacuation occupancy is elevated",
                    "Current total center occupancy is high and should be monitored for overflow risk.",
                    "Open evacuation centers",
                    LocalDateTime.now().toString(),
                    "Evacuation Centers",
                    "Monitor evacuation load distribution"
            ));
        }
    }

    private void addBudgetAlerts(List<ActiveAlertResponse> alerts,
                                 BudgetCurrentSummaryResponse budget,
                                 ResourcesReadinessSummaryResponse resources) {
        if (budget.utilizationRate() >= 90) {
            alerts.add(new ActiveAlertResponse(
                    "BUDGET",
                    "CRITICAL",
                    "ACTIVE",
                    "Budget utilization is at critical level",
                    "Current obligations have consumed most of the available annual budget.",
                    "Current year budget",
                    LocalDateTime.now().toString(),
                    "Budget",
                    "Review response spending priorities immediately"
            ));
        } else if (budget.utilizationRate() >= 75) {
            alerts.add(new ActiveAlertResponse(
                    "BUDGET",
                    "HIGH",
                    "ACTIVE",
                    "Budget utilization is high",
                    "Operational spending is approaching a level that may reduce response flexibility.",
                    "Current year budget",
                    LocalDateTime.now().toString(),
                    "Budget",
                    "Review non-critical expenditures"
            ));
        }

        if (isAnyStatus(resources.budgetRiskLevel(), "HIGH", "CRITICAL")) {
            alerts.add(new ActiveAlertResponse(
                    "BUDGET",
                    "MEDIUM",
                    "WATCH",
                    "Budget pressure may affect sustained readiness",
                    "Budget readiness indicators suggest tighter flexibility if operational demand increases.",
                    "Current year budget",
                    LocalDateTime.now().toString(),
                    "Budget",
                    "Track resource usage against remaining budget"
            ));
        }
    }

    private List<PriorityActionResponse> buildPriorityActions(List<ActiveAlertResponse> activeAlerts) {
        List<PriorityActionResponse> rows = new ArrayList<>();

        activeAlerts.stream()
                .filter(alert -> isAnyStatus(alert.severity(), "CRITICAL", "HIGH"))
                .limit(5)
                .forEach(alert -> rows.add(new PriorityActionResponse(
                        alert.title(),
                        safe(alert.recommendation())
                )));

        if (rows.isEmpty()) {
            rows.add(new PriorityActionResponse(
                    "Maintain current readiness monitoring",
                    "No high-priority immediate action is required from current warning signals."
            ));
        }

        return rows;
    }

    private List<WarningHistoryResponse> buildRecentHistory() {
        return operationHistoryRepository.findAll().stream()
                .sorted(Comparator.comparing(OperationHistory::getPerformedAt).reversed())
                .limit(6)
                .map(history -> new WarningHistoryResponse(
                        safe(history.getActionType()),
                        safe(history.getDescription()),
                        safe(history.getToStatus()),
                        history.getPerformedAt() != null ? history.getPerformedAt().toString() : null
                ))
                .toList();
    }

    private List<ReadinessNoteResponse> buildReadinessNotes(MunicipalWeatherForecastResponse weather,
                                                            ResourcesReadinessSummaryResponse resources,
                                                            BudgetCurrentSummaryResponse budget,
                                                            List<User> availableResponders,
                                                            long activeIncidents,
                                                            long activeCalamities,
                                                            List<ActiveAlertResponse> activeAlerts) {
        List<ReadinessNoteResponse> notes = new ArrayList<>();

        notes.add(new ReadinessNoteResponse(
                "Overall Readiness Observation",
                "Current overall readiness is " + mapOverallReadiness(resources.overallReadinessRiskLevel()).toLowerCase(Locale.ROOT)
                        + " based on weather, resources, evacuation, budget, and personnel signals.",
                "Readiness Summary"
        ));

        if (weather.summary() != null) {
            notes.add(new ReadinessNoteResponse(
                    "Weather Observation",
                    safe(weather.summary().recommendation()),
                    "Weather"
            ));
        }

        if (!resources.warnings().isEmpty()) {
            notes.add(new ReadinessNoteResponse(
                    "Resource Observation",
                    resources.warnings().get(0),
                    "Resources"
            ));
        }

        notes.add(new ReadinessNoteResponse(
                "Operations Observation",
                "There are " + activeIncidents + " active incident(s), "
                        + activeCalamities + " active calamity record(s), and "
                        + availableResponders.size() + " currently available responder(s).",
                "Operations"
        ));

        if (budget.utilizationRate() >= 75) {
            notes.add(new ReadinessNoteResponse(
                    "Budget Observation",
                    "Budget utilization is elevated and should be considered in operational planning.",
                    "Budget"
            ));
        }

        if (activeAlerts.isEmpty()) {
            notes.add(new ReadinessNoteResponse(
                    "Warning Observation",
                    "No significant warning signals were generated from the current monitoring snapshot.",
                    "Alerts"
            ));
        }

        return notes;
    }

    private boolean isLowOrOutOfStock(Inventory inventory) {
        int reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0;
        return inventory.getAvailableQuantity() <= reorderLevel;
    }

    private String mapRiskToReadiness(String riskLevel) {
        return switch (normalize(riskLevel)) {
            case "LOW" -> "READY";
            case "MODERATE", "MEDIUM", "HIGH" -> "LIMITED";
            case "CRITICAL", "SEVERE" -> "CRITICAL";
            default -> "LIMITED";
        };
    }

    private String mapOverallReadiness(String riskLevel) {
        return switch (normalize(riskLevel)) {
            case "LOW" -> "Ready";
            case "MODERATE", "MEDIUM", "HIGH" -> "Partially Ready";
            case "CRITICAL", "SEVERE" -> "Not Ready";
            default -> "Partially Ready";
        };
    }

    private int domainScoreFromRisk(String riskLevel) {
        return switch (normalize(riskLevel)) {
            case "LOW" -> 88;
            case "MODERATE", "MEDIUM" -> 66;
            case "HIGH" -> 48;
            case "CRITICAL", "SEVERE" -> 25;
            default -> 60;
        };
    }

    private int severityRank(String severity) {
        return switch (normalize(severity)) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    private boolean isAnyStatus(String value, String... expected) {
        String actual = normalize(value);
        for (String candidate : expected) {
            if (actual.equals(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
