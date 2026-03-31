package com.isufst.mdrrmosystem.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AlertsWarningsService {

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
        MunicipalWeatherForecastResponse weather = weatherForecastService.getMunicipalForecast();
        ResourcesReadinessSummaryResponse resources = resourcesReadinessService.getReadinessSummary();
        BudgetCurrentSummaryResponse budget = budgetService.getCurrentSummary();

        List<User> availableResponders = userRepository.findAvailableResponders();
        List<EvacuationActivation> openCenters = evacuationActivationRepository.findByStatus("OPEN");
        List<Inventory> inventory = inventoryRepository.findAll();

        long activeIncidents =
                incidentRepository.countByStatus("ONGOING")
                        + incidentRepository.countByStatus("IN_PROGRESS")
                        + incidentRepository.countByStatus("ON_SITE");

        long activeCalamities = calamityRepository.findAll().stream()
                .filter(c -> isAnyStatus(c.getStatus(), "ACTIVE", "MONITORING"))
                .count();

        List<AlertsReadinessDomainResponse> readinessDomains = buildReadinessDomains(
                weather,
                resources,
                budget,
                availableResponders,
                openCenters,
                activeIncidents,
                activeCalamities,
                inventory
        );

        List<ActiveAlertResponse> activeAlerts = buildActiveAlerts(
                weather,
                resources,
                budget,
                availableResponders,
                openCenters,
                activeIncidents,
                activeCalamities,
                inventory
        );

        List<PriorityActionResponse> priorityActions = buildPriorityActions(activeAlerts);
        List<WarningHistoryResponse> recentHistory = buildRecentHistory();
        List<ReadinessNoteResponse> readinessNotes = buildReadinessNotes(
                weather,
                resources,
                budget,
                availableResponders,
                activeIncidents,
                activeCalamities,
                activeAlerts,
                readinessDomains
        );

        String overallReadinessLabel = deriveOverallReadiness(readinessDomains);

        int criticalGapsCount = (int) activeAlerts.stream()
                .filter(alert -> isAnyStatus(alert.severity(), "HIGH", "CRITICAL"))
                .count();

        String responseCapacityLabel = availableResponders.size() + " Available";

        AlertsWarningsSummaryResponse summary = new AlertsWarningsSummaryResponse(
                overallReadinessLabel,
                activeAlerts.size(),
                criticalGapsCount,
                responseCapacityLabel
        );

        return new AlertsWarningsOverviewResponse(
                LocalDateTime.now().toString(),
                summary,
                readinessDomains,
                activeAlerts,
                priorityActions,
                recentHistory,
                readinessNotes
        );
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
        rows.add(buildEvacuationDomain(resources, openCenters, activeIncidents, activeCalamities));
        rows.add(buildBudgetDomain(resources, budget));

        return rows;
    }

    private AlertsReadinessDomainResponse buildWeatherDomain(MunicipalWeatherForecastResponse weather) {
        String weatherRisk = weather.summary() != null ? weather.summary().overallRiskLevel() : "MEDIUM";
        String readinessStatus = mapWeatherRiskToReadiness(weatherRisk);

        int highRiskBarangays = weather.summary() != null ? weather.summary().highRiskBarangays() : 0;
        int totalBarangays = weather.summary() != null ? weather.summary().totalBarangays() : 0;

        int score = switch (normalize(weatherRisk)) {
            case "LOW" -> 88;
            case "MEDIUM", "MODERATE" -> 68;
            case "HIGH" -> 45;
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
        int projectedNeed = estimateResponderNeed(activeIncidents, activeCalamities);

        String status;
        int score;
        String note;

        if (available <= 0 || available < projectedNeed) {
            status = "NOT READY";
            score = 20;
            note = "Available responders are below projected operational need.";
        } else if (available < projectedNeed + 3) {
            status = "LIMITED";
            score = 55;
            note = "Responder coverage is available but reserve depth is thin.";
        } else {
            status = "READY";
            score = 85;
            note = "Responder availability is adequate for current operational demand.";
        }

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Available Responders", String.valueOf(available)),
                new AlertsMetricResponse("Projected Need", String.valueOf(projectedNeed)),
                new AlertsMetricResponse("Active Incidents", String.valueOf(activeIncidents)),
                new AlertsMetricResponse("Active Calamities", String.valueOf(activeCalamities))
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
        List<Inventory> items = inventory == null ? List.of() : inventory;

        int totalItems = items.size();

        long availableItems = items.stream()
                .filter(i -> i.getAvailableQuantity() > 0)
                .count();

        long criticalAvailable = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(i -> i.getAvailableQuantity() > 0)
                .count();

        long distinctCategories = items.stream()
                .map(i -> normalize(i.getCategory()))
                .filter(s -> !s.isBlank())
                .distinct()
                .count();

        long lowOrOut = items.stream()
                .filter(this::isLowOrOutOfStock)
                .count();

        String status;
        int score;
        String note;

        if (items.isEmpty() || availableItems == 0) {
            status = "NOT READY";
            score = 0;
            note = "No usable inventory is available for emergency response.";
        } else if (distinctCategories < 4 || criticalAvailable < 3 || availableItems < 5) {
            status = "NOT READY";
            score = 20;
            note = "Inventory exists but is too limited in quantity or category coverage for emergency operations.";
        } else if (lowOrOut >= Math.max(2, totalItems / 3)) {
            status = "LIMITED";
            score = 50;
            note = "Inventory is available but several items are already low or at risk.";
        } else {
            status = "READY";
            score = 85;
            note = "Inventory coverage is currently adequate for routine operational readiness.";
        }

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Inventory Records", String.valueOf(totalItems)),
                new AlertsMetricResponse("Usable Items", String.valueOf(availableItems)),
                new AlertsMetricResponse("Category Coverage", String.valueOf(distinctCategories)),
                new AlertsMetricResponse("Critical Items Ready", String.valueOf(criticalAvailable))
        );

        return new AlertsReadinessDomainResponse(
                "RESOURCE",
                "Equipment & Supply Readiness",
                "Inventory condition, rescue support items, and emergency response stock availability.",
                status,
                score,
                note,
                metrics
        );
    }

    private AlertsReadinessDomainResponse buildEvacuationDomain(ResourcesReadinessSummaryResponse resources,
                                                                List<EvacuationActivation> openCenters,
                                                                long activeIncidents,
                                                                long activeCalamities) {
        List<EvacuationActivation> centers = openCenters == null ? List.of() : openCenters;

        int openCount = centers.size();
        int availableSlots = centers.stream()
                .filter(a -> a.getCenter() != null)
                .mapToInt(a -> Math.max(a.getCenter().getCapacity() - a.getCurrentEvacuees(), 0))
                .sum();

        int projectedDemand = estimateProjectedEvacuationDemand(activeIncidents, activeCalamities);

        String status;
        int score;
        String note;

        if (openCount == 0 || availableSlots <= 0) {
            status = "NOT READY";
            score = 0;
            note = "No evacuation center capacity is currently available.";
        } else if (availableSlots < projectedDemand) {
            status = "NOT READY";
            score = 25;
            note = "Current evacuation capacity is below projected operational demand.";
        } else if (resources.nearFullCentersCount() > 0 || availableSlots < Math.ceil(projectedDemand * 1.5)) {
            status = "LIMITED";
            score = 55;
            note = "Evacuation capacity is available but may tighten quickly if conditions escalate.";
        } else {
            status = "READY";
            score = 85;
            note = "Evacuation center capacity is currently adequate for projected displacement needs.";
        }

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Open Centers", String.valueOf(openCount)),
                new AlertsMetricResponse("Available Slots", String.valueOf(availableSlots)),
                new AlertsMetricResponse("Near Full Centers", String.valueOf(resources.nearFullCentersCount())),
                new AlertsMetricResponse("Projected Demand", String.valueOf(projectedDemand))
        );

        return new AlertsReadinessDomainResponse(
                "EVACUATION",
                "Evacuation Readiness",
                "Current center capacity, occupancy pressure, and displacement support readiness.",
                status,
                score,
                note,
                metrics
        );
    }

    private AlertsReadinessDomainResponse buildBudgetDomain(ResourcesReadinessSummaryResponse resources,
                                                            BudgetCurrentSummaryResponse budget) {
        double utilization = budget.utilizationRate();
        double remaining = budget.totalRemaining();

        String status;
        int score;
        String note;

        if (remaining <= 0 || utilization >= 95) {
            status = "NOT READY";
            score = 10;
            note = "Available budget is critically insufficient for continued operations.";
        } else if (utilization >= 80) {
            status = "LIMITED";
            score = 50;
            note = "Budget utilization is high and may constrain prolonged response.";
        } else {
            status = "READY";
            score = 82;
            note = "Current budget remains usable for operational support.";
        }

        List<AlertsMetricResponse> metrics = List.of(
                new AlertsMetricResponse("Budget Risk", safe(resources.budgetRiskLevel())),
                new AlertsMetricResponse("Utilization Rate", String.format(Locale.US, "%.0f%%", utilization)),
                new AlertsMetricResponse("Remaining Budget", String.format(Locale.US, "₱ %,.2f", remaining)),
                new AlertsMetricResponse("Current Year", String.valueOf(budget.year()))
        );

        return new AlertsReadinessDomainResponse(
                "BUDGET",
                "Budget Readiness",
                "Financial flexibility available for current and near-term operations.",
                status,
                score,
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
        addEvacuationAlerts(alerts, resources, openCenters, activeIncidents, activeCalamities);
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
        List<Inventory> items = inventory == null ? List.of() : inventory;

        if (items.isEmpty()) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    "CRITICAL",
                    "ACTIVE",
                    "No inventory is available for emergency response",
                    "The system has no usable inventory records for emergency supplies or operational equipment.",
                    "Municipal inventory",
                    LocalDateTime.now().toString(),
                    "Resources",
                    "Record and prepare essential emergency inventory immediately"
            ));
            return;
        }

        long criticalOutOfStock = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(i -> i.getAvailableQuantity() <= 0)
                .count();

        long criticalLowStock = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(this::isLowOrOutOfStock)
                .count();

        long distinctCategories = items.stream()
                .map(i -> normalize(i.getCategory()))
                .filter(s -> !s.isBlank())
                .distinct()
                .count();

        long usableItems = items.stream()
                .filter(i -> i.getAvailableQuantity() > 0)
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

        if (distinctCategories < 4 || usableItems < 5) {
            alerts.add(new ActiveAlertResponse(
                    "RESOURCE",
                    "HIGH",
                    "ACTIVE",
                    "Inventory coverage is too limited for emergency operations",
                    "Available inventory is too narrow in category coverage or usable stock depth for broader incident and calamity response.",
                    "Municipal inventory",
                    LocalDateTime.now().toString(),
                    "Resources",
                    "Expand essential inventory coverage beyond a few item types"
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
        int projectedNeed = estimateResponderNeed(activeIncidents, activeCalamities);

        if (available <= 0 || available < projectedNeed) {
            alerts.add(new ActiveAlertResponse(
                    "PERSONNEL",
                    "CRITICAL",
                    "ACTIVE",
                    "Responder availability is below operational need",
                    "Available responders are below the projected requirement for current active incidents and calamities.",
                    "MDRRMO personnel pool",
                    LocalDateTime.now().toString(),
                    "Operations",
                    "Review standby roster and reserve assignments"
            ));
        } else if (available < projectedNeed + 3) {
            alerts.add(new ActiveAlertResponse(
                    "PERSONNEL",
                    "MEDIUM",
                    "WATCH",
                    "Responder reserve capacity is limited",
                    "Current responder coverage is available but reserve depth is thin for escalation.",
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
                                     List<EvacuationActivation> openCenters,
                                     long activeIncidents,
                                     long activeCalamities) {
        List<EvacuationActivation> centers = openCenters == null ? List.of() : openCenters;

        int availableSlots = centers.stream()
                .filter(a -> a.getCenter() != null)
                .mapToInt(a -> Math.max(a.getCenter().getCapacity() - a.getCurrentEvacuees(), 0))
                .sum();

        int projectedDemand = estimateProjectedEvacuationDemand(activeIncidents, activeCalamities);

        if (centers.isEmpty() || availableSlots <= 0) {
            alerts.add(new ActiveAlertResponse(
                    "EVACUATION",
                    "CRITICAL",
                    "ACTIVE",
                    "No evacuation center capacity is available",
                    "There are no open evacuation centers with usable capacity for emergency displacement support.",
                    "Evacuation centers",
                    LocalDateTime.now().toString(),
                    "Evacuation Centers",
                    "Activate and prepare evacuation centers immediately"
            ));
            return;
        }

        if (availableSlots < projectedDemand) {
            alerts.add(new ActiveAlertResponse(
                    "EVACUATION",
                    "CRITICAL",
                    "ACTIVE",
                    "Evacuation capacity is below projected demand",
                    "Current evacuation center capacity may be insufficient if conditions worsen or displacement increases.",
                    "Open evacuation centers",
                    LocalDateTime.now().toString(),
                    "Evacuation Centers",
                    "Prepare additional evacuation capacity"
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

        if (!centers.isEmpty() && resources.overallOccupancyRate() >= 80) {
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
        if (budget.totalRemaining() <= 0 || budget.utilizationRate() >= 95) {
            alerts.add(new ActiveAlertResponse(
                    "BUDGET",
                    "CRITICAL",
                    "ACTIVE",
                    "Budget readiness is critically constrained",
                    "Current budget availability may not support extended or escalating response operations.",
                    "Current year budget",
                    LocalDateTime.now().toString(),
                    "Budget",
                    "Review response spending priorities immediately"
            ));
        } else if (budget.utilizationRate() >= 80) {
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
                                                            List<ActiveAlertResponse> activeAlerts,
                                                            List<AlertsReadinessDomainResponse> readinessDomains) {
        List<ReadinessNoteResponse> notes = new ArrayList<>();

        notes.add(new ReadinessNoteResponse(
                "Overall Readiness Observation",
                "Current overall readiness is " + deriveOverallReadiness(readinessDomains).toLowerCase(Locale.ROOT)
                        + " based on weather, resources, evacuation, budget, and personnel sufficiency.",
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

        if (budget.utilizationRate() >= 80) {
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

    private String deriveOverallReadiness(List<AlertsReadinessDomainResponse> domains) {
        boolean hasNotReady = domains.stream()
                .anyMatch(d -> isAnyStatus(d.status(), "NOT READY"));

        if (hasNotReady) {
            return "Not Ready";
        }

        boolean hasLimited = domains.stream()
                .anyMatch(d -> isAnyStatus(d.status(), "LIMITED"));

        if (hasLimited) {
            return "Partially Ready";
        }

        return "Ready";
    }

    private int estimateResponderNeed(long activeIncidents, long activeCalamities) {
        int incidentNeed = (int) activeIncidents * 2;
        int calamityNeed = (int) activeCalamities * 4;
        return Math.max(4, incidentNeed + calamityNeed);
    }

    private int estimateProjectedEvacuationDemand(long activeIncidents, long activeCalamities) {
        int incidentDemand = (int) activeIncidents * 10;
        int calamityDemand = (int) activeCalamities * 50;
        return Math.max(20, incidentDemand + calamityDemand);
    }

    private boolean isLowOrOutOfStock(Inventory inventory) {
        int reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0;
        return inventory.getAvailableQuantity() <= reorderLevel;
    }

    private String mapWeatherRiskToReadiness(String riskLevel) {
        return switch (normalize(riskLevel)) {
            case "LOW" -> "READY";
            case "MODERATE", "MEDIUM" -> "LIMITED";
            case "HIGH", "CRITICAL", "SEVERE" -> "NOT READY";
            default -> "LIMITED";
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }
}