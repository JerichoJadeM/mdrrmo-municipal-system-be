package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.*;
import com.isufst.mdrrmosystem.repository.*;
import com.isufst.mdrrmosystem.response.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperationalForecastService {

    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final InventoryRepository inventoryRepository;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final EvacuationCenterRepository evacuationCenterRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final ForecastRuleRegistry forecastRuleRegistry;

    public OperationalForecastService(IncidentRepository incidentRepository,
                                      CalamityRepository calamityRepository,
                                      InventoryRepository inventoryRepository,
                                      ReliefDistributionRepository reliefDistributionRepository,
                                      EvacuationActivationRepository evacuationActivationRepository,
                                      EvacuationCenterRepository evacuationCenterRepository,
                                      BudgetRepository budgetRepository,
                                      ExpenseRepository expenseRepository,
                                      ForecastRuleRegistry forecastRuleRegistry) {
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.inventoryRepository = inventoryRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.evacuationCenterRepository = evacuationCenterRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.forecastRuleRegistry = forecastRuleRegistry;
    }

    @Transactional(readOnly = true)
    public OperationalForecastResponse forecastIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        IncidentForecastTemplate template = forecastRuleRegistry.getIncidentTemplate(
                incident.getType(),
                incident.getSeverity()
        );

        List<ResourceRecommendationResponse> recommendations = buildIncidentRecommendations(template);
        List<StockCheckResponse> stockChecks = buildStockChecks(template.getItems());
        ReliefReadinessResponse reliefReadiness = buildIncidentReliefReadiness(template);
        List<EvacuationCheckResponse> evacuationChecks = buildIncidentEvacuationChecks(template);

        List<CostDriverResponse> costDrivers = buildIncidentCostDrivers(template);
        double forecastedBudget = sumCostDrivers(costDrivers);
        double actualCostToDate = computeIncidentActualCostToDate(incidentId);
        double variance = forecastedBudget - actualCostToDate;

        List<BudgetWarningResponse> warnings = buildWarnings(
                forecastedBudget,
                stockChecks,
                reliefReadiness,
                evacuationChecks
        );

        return new OperationalForecastResponse(
                "INCIDENT",
                incident.getId(),
                incident.getType(),
                incident.getSeverity(),
                incident.getStatus(),
                template.isEvacuationRecommended(),
                template.isReliefRecommended(),
                round2(forecastedBudget),
                round2(actualCostToDate),
                round2(variance),
                recommendations,
                stockChecks,
                reliefReadiness,
                evacuationChecks,
                costDrivers,
                warnings
        );
    }

    @Transactional(readOnly = true)
    public OperationalForecastResponse forecastCalamity(Long calamityId) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found"));

        CalamityForecastTemplate template = forecastRuleRegistry.getCalamityTemplate(
                calamity.getType(),
                calamity.getSeverity()
        );

        List<ResourceRecommendationResponse> recommendations = buildCalamityRecommendations(template);
        List<StockCheckResponse> stockChecks = buildStockChecks(template.getItems());
        ReliefReadinessResponse reliefReadiness = buildCalamityReliefReadiness(template);
        List<EvacuationCheckResponse> evacuationChecks = buildCalamityEvacuationChecks(template);

        List<CostDriverResponse> costDrivers = buildCalamityCostDrivers(template);
        double forecastedBudget = sumCostDrivers(costDrivers);

        // actual cost for calamity is limited for now because current actual transaction model is incident-linked
        double actualCostToDate = 0;
        double variance = forecastedBudget - actualCostToDate;

        List<BudgetWarningResponse> warnings = buildWarnings(
                forecastedBudget,
                stockChecks,
                reliefReadiness,
                evacuationChecks
        );

        return new OperationalForecastResponse(
                "CALAMITY",
                calamity.getId(),
                calamity.getType(),
                calamity.getSeverity(),
                calamity.getStatus(),
                template.isEvacuationRecommended(),
                template.isReliefRecommended(),
                round2(forecastedBudget),
                round2(actualCostToDate),
                round2(variance),
                recommendations,
                stockChecks,
                reliefReadiness,
                evacuationChecks,
                costDrivers,
                warnings
        );
    }

    private List<ResourceRecommendationResponse> buildIncidentRecommendations(IncidentForecastTemplate template) {
        List<ResourceRecommendationResponse> result = new ArrayList<>();

        result.add(new ResourceRecommendationResponse(
                "PERSONNEL",
                "Responders",
                "PERSONNEL",
                template.getSuggestedResponders(),
                "persons",
                "Suggested field personnel"
        ));

        result.add(new ResourceRecommendationResponse(
                "VEHICLE",
                "Vehicle Support",
                "VEHICLE",
                1,
                "unit",
                "Suggested transport/logistics support"
        ));

        for (ForecastTemplateItem item : template.getItems()) {
            result.add(toRecommendation(item));
        }

        return result;
    }

    private List<ResourceRecommendationResponse> buildCalamityRecommendations(CalamityForecastTemplate template) {
        List<ResourceRecommendationResponse> result = new ArrayList<>();

        result.add(new ResourceRecommendationResponse(
                "PERSONNEL",
                "Response Team",
                "PERSONNEL",
                1,
                "team",
                "Suggested calamity response team"
        ));

        result.add(new ResourceRecommendationResponse(
                "VEHICLE",
                "Vehicle Support",
                "VEHICLE",
                1,
                "unit",
                "Suggested transport/logistics support"
        ));

        for (ForecastTemplateItem item : template.getItems()) {
            result.add(toRecommendation(item));
        }

        return result;
    }

    private ResourceRecommendationResponse toRecommendation(ForecastTemplateItem item) {
        return new ResourceRecommendationResponse(
                item.getResourceType(),
                item.getItemName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnit(),
                item.getReason()
        );
    }

    private List<StockCheckResponse> buildStockChecks(List<ForecastTemplateItem> templateItems) {
        return templateItems.stream()
                .map(this::mapTemplateItemToStockCheck)
                .toList();
    }

    private StockCheckResponse mapTemplateItemToStockCheck(ForecastTemplateItem item) {
        List<Inventory> allInventory = inventoryRepository.findAll();

        Optional<Inventory> exactMatch = allInventory.stream()
                .filter(inv -> inv.getName() != null)
                .filter(inv -> inv.getName().trim().equalsIgnoreCase(item.getItemName()))
                .findFirst();

        if (exactMatch.isPresent()) {
            Inventory inventory = exactMatch.get();
            return new StockCheckResponse(
                    inventory.getId(),
                    inventory.getName(),
                    inventory.getCategory(),
                    item.getQuantity(),
                    inventory.getAvailableQuantity(),
                    inventory.getUnit(),
                    resolveStockStatus(item.getQuantity(), inventory.getAvailableQuantity())
            );
        }

        Optional<Inventory> categoryMatch = allInventory.stream()
                .filter(inv -> inv.getCategory() != null)
                .filter(inv -> inv.getCategory().trim().equalsIgnoreCase(item.getCategory()))
                .findFirst();

        if (categoryMatch.isPresent()) {
            Inventory inventory = categoryMatch.get();
            return new StockCheckResponse(
                    inventory.getId(),
                    item.getItemName(),
                    inventory.getCategory(),
                    item.getQuantity(),
                    inventory.getAvailableQuantity(),
                    inventory.getUnit(),
                    resolveStockStatus(item.getQuantity(), inventory.getAvailableQuantity())
            );
        }

        return new StockCheckResponse(
                null,
                item.getItemName(),
                item.getCategory(),
                item.getQuantity(),
                0,
                item.getUnit(),
                "NOT_FOUND"
        );
    }

    private String resolveStockStatus(int required, int available) {
        if (available <= 0) {
            return "OUT_OF_STOCK";
        }
        if (available < required) {
            return "LOW_STOCK";
        }
        return "AVAILABLE";
    }

    private ReliefReadinessResponse buildIncidentReliefReadiness(IncidentForecastTemplate template) {
        if (!template.isReliefRecommended()) {
            return new ReliefReadinessResponse(false, 0, 0, List.of());
        }

        List<ForecastTemplateItem> reliefItems = template.getItems().stream()
                .filter(item -> "RELIEF".equalsIgnoreCase(item.getResourceType()))
                .toList();

        return new ReliefReadinessResponse(
                true,
                template.getProjectedEvacuees(),
                template.getProjectedReliefPacks(),
                buildStockChecks(reliefItems)
        );
    }

    private ReliefReadinessResponse buildCalamityReliefReadiness(CalamityForecastTemplate template) {
        if (!template.isReliefRecommended()) {
            return new ReliefReadinessResponse(false, 0, 0, List.of());
        }

        List<ForecastTemplateItem> reliefItems = template.getItems().stream()
                .filter(item -> "RELIEF".equalsIgnoreCase(item.getResourceType()))
                .toList();

        return new ReliefReadinessResponse(
                true,
                template.getProjectedEvacuees(),
                template.getProjectedReliefPacks(),
                buildStockChecks(reliefItems)
        );
    }

    private List<EvacuationCheckResponse> buildIncidentEvacuationChecks(IncidentForecastTemplate template) {
        if (!template.isEvacuationRecommended()) {
            return List.of();
        }

        return evacuationCenterRepository.findAll().stream()
                .map(this::mapCenterToEvacuationCheck)
                .toList();
    }

    private List<EvacuationCheckResponse> buildCalamityEvacuationChecks(CalamityForecastTemplate template) {
        if (!template.isEvacuationRecommended()) {
            return List.of();
        }

        return evacuationCenterRepository.findAll().stream()
                .map(this::mapCenterToEvacuationCheck)
                .toList();
    }

    private EvacuationCheckResponse mapCenterToEvacuationCheck(EvacuationCenter center) {
        int activeEvacuees = evacuationActivationRepository.findByStatus("OPEN").stream()
                .filter(a -> a.getCenter() != null && Objects.equals(a.getCenter().getId(), center.getId()))
                .mapToInt(EvacuationActivation::getCurrentEvacuees)
                .sum();

        int availableSlots = Math.max(0, center.getCapacity() - activeEvacuees);

        String status;
        if (availableSlots <= 0) {
            status = "FULL";
        } else if (availableSlots <= Math.max(1, center.getCapacity() * 0.2)) {
            status = "NEAR_CAPACITY";
        } else {
            status = "AVAILABLE";
        }

        return new EvacuationCheckResponse(
                center.getId(),
                center.getName(),
                center.getBarangay() != null ? center.getBarangay().getName() : "-",
                center.getCapacity(),
                activeEvacuees,
                availableSlots,
                status
        );
    }

    private List<CostDriverResponse> buildIncidentCostDrivers(IncidentForecastTemplate template) {
        double equipmentCost = sumTemplateItemsByType(template.getItems(), "EQUIPMENT", "MEDICAL");
        double reliefCost = sumTemplateItemsByType(template.getItems(), "RELIEF");
        double evacuationCost = template.isEvacuationRecommended()
                ? template.getProjectedEvacuees() * 150
                : 0;
        double personnelCost = template.getPersonnelEstimate();
        double vehicleCost = template.getVehicleEstimate();

        double subtotal = equipmentCost + reliefCost + evacuationCost + personnelCost + vehicleCost;
        double contingency = subtotal * template.getContingencyPercent();

        return List.of(
                new CostDriverResponse("Equipment", round2(equipmentCost)),
                new CostDriverResponse("Relief Goods", round2(reliefCost)),
                new CostDriverResponse("Evacuation", round2(evacuationCost)),
                new CostDriverResponse("Personnel", round2(personnelCost)),
                new CostDriverResponse("Vehicle", round2(vehicleCost)),
                new CostDriverResponse("Contingency", round2(contingency))
        );
    }

    private List<CostDriverResponse> buildCalamityCostDrivers(CalamityForecastTemplate template) {
        double equipmentCost = sumTemplateItemsByType(template.getItems(), "EQUIPMENT", "MEDICAL");
        double reliefCost = sumTemplateItemsByType(template.getItems(), "RELIEF");
        double evacuationCost = template.isEvacuationRecommended()
                ? template.getEvacuationDailyEstimate() * estimatedDays(template.getSeverity())
                : 0;
        double personnelCost = template.getPersonnelEstimate();
        double vehicleCost = template.getVehicleEstimate();

        double subtotal = equipmentCost + reliefCost + evacuationCost + personnelCost + vehicleCost;
        double contingency = subtotal * template.getContingencyPercent();

        return List.of(
                new CostDriverResponse("Equipment", round2(equipmentCost)),
                new CostDriverResponse("Relief Goods", round2(reliefCost)),
                new CostDriverResponse("Evacuation", round2(evacuationCost)),
                new CostDriverResponse("Personnel", round2(personnelCost)),
                new CostDriverResponse("Vehicle", round2(vehicleCost)),
                new CostDriverResponse("Contingency", round2(contingency))
        );
    }

    private double sumTemplateItemsByType(List<ForecastTemplateItem> items, String... types) {
        Set<String> allowed = Arrays.stream(types)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(item -> allowed.contains(item.getResourceType().toUpperCase()))
                .mapToDouble(item -> item.getQuantity() * item.getEstimatedUnitCost())
                .sum();
    }

    private int estimatedDays(String severity) {
        String normalized = severity == null ? "" : severity.trim().toUpperCase();
        return switch (normalized) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private double computeIncidentActualCostToDate(Long incidentId) {
        double expenseCost = Optional.ofNullable(expenseRepository.sumByIncidentId(incidentId)).orElse(0.0);

        double reliefCost = reliefDistributionRepository.findByIncident_Id(incidentId).stream()
                .mapToDouble(this::estimateReliefDistributionCost)
                .sum();

        double evacuationCost = evacuationActivationRepository.findByIncident_Id(incidentId).stream()
                .mapToDouble(this::estimateEvacuationActivationCost)
                .sum();

        return round2(expenseCost + reliefCost + evacuationCost);
    }

    private double estimateReliefDistributionCost(ReliefDistribution reliefDistribution) {
        Inventory inventory = reliefDistribution.getInventory();
        if (inventory == null) {
            return 0;
        }

        double unitCost = estimateInventoryUnitCost(inventory);
        return unitCost * reliefDistribution.getQuantity();
    }

    private double estimateEvacuationActivationCost(EvacuationActivation activation) {
        LocalDateTime openedAt = activation.getOpenedAt();
        if (openedAt == null) {
            return 0;
        }

        LocalDateTime end = activation.getClosedAt() != null ? activation.getClosedAt() : LocalDateTime.now();
        long days = Math.max(1, ChronoUnit.DAYS.between(openedAt.toLocalDate(), end.toLocalDate()) + 1);

        return activation.getCurrentEvacuees() * 150.0 * days;
    }

    private double estimateInventoryUnitCost(Inventory inventory) {
        String name = inventory.getName() == null ? "" : inventory.getName().trim().toUpperCase();
        String category = inventory.getCategory() == null ? "" : inventory.getCategory().trim().toUpperCase();

        if (name.contains("FOOD PACK")) return 950;
        if (name.contains("WATER")) return 30;
        if (name.contains("HYGIENE")) return 150;
        if (name.contains("BLANKET")) return 300;
        if (name.contains("FLASHLIGHT")) return 500;
        if (name.contains("FIRST AID")) return 1200;
        if (name.contains("STRETCHER")) return 2500;
        if (name.contains("HELMET")) return 1200;
        if (name.contains("GLOVE")) return 250;
        if (name.contains("ROPE")) return 800;
        if (name.contains("CHAINSAW")) return 3500;
        if (name.contains("OXYGEN")) return 3000;

        return switch (category) {
            case "FOOD" -> 100;
            case "MEDICAL" -> 800;
            case "RESCUE EQUIPMENT" -> 1200;
            case "TOOL" -> 600;
            default -> 500;
        };
    }

    private List<BudgetWarningResponse> buildWarnings(double forecastedBudget,
                                                      List<StockCheckResponse> stockChecks,
                                                      ReliefReadinessResponse reliefReadiness,
                                                      List<EvacuationCheckResponse> evacuationChecks) {
        List<BudgetWarningResponse> warnings = new ArrayList<>();

        boolean hasLowStock = stockChecks.stream().anyMatch(s -> "LOW_STOCK".equalsIgnoreCase(s.status()));
        boolean hasOutOfStock = stockChecks.stream().anyMatch(s ->
                "OUT_OF_STOCK".equalsIgnoreCase(s.status()) || "NOT_FOUND".equalsIgnoreCase(s.status()));

        if (hasOutOfStock) {
            warnings.add(new BudgetWarningResponse("CRITICAL", "Critical inventory items are unavailable."));
        } else if (hasLowStock) {
            warnings.add(new BudgetWarningResponse("WARNING", "Some required inventory items are low in stock."));
        }

        boolean reliefInsufficient = reliefReadiness.reliefStockChecks().stream().anyMatch(s ->
                !"AVAILABLE".equalsIgnoreCase(s.status()));

        if (reliefReadiness.recommended() && reliefInsufficient) {
            warnings.add(new BudgetWarningResponse("CRITICAL", "Relief stock may be insufficient for projected demand."));
        }

        if (!evacuationChecks.isEmpty()) {
            boolean noAvailableCenter = evacuationChecks.stream().noneMatch(e -> "AVAILABLE".equalsIgnoreCase(e.status()));
            if (noAvailableCenter) {
                warnings.add(new BudgetWarningResponse("CRITICAL", "No evacuation center has enough available capacity."));
            }
        }

        List<Budget> currentYearBudgets = budgetRepository.findByYear(LocalDate.now().getYear());

        if (!currentYearBudgets.isEmpty()) {
            double totalBudgetAmount = currentYearBudgets.stream()
                    .mapToDouble(Budget::getTotalAmount)
                    .sum();

            double totalSpent = currentYearBudgets.stream()
                    .mapToDouble(budget -> Optional.ofNullable(expenseRepository.sumByBudgetId(budget.getId())).orElse(0.0))
                    .sum();

            double remaining = totalBudgetAmount - totalSpent;

            if (forecastedBudget > remaining) {
                warnings.add(new BudgetWarningResponse("CRITICAL", "Forecast exceeds remaining annual budget."));
            } else if (remaining > 0 && forecastedBudget >= remaining * 0.80) {
                warnings.add(new BudgetWarningResponse("WARNING", "Forecast is close to the remaining annual budget."));
            }
        } else {
            warnings.add(new BudgetWarningResponse("INFO", "No current-year budget found for budget sufficiency check."));
        }

        return warnings;
    }

    private double sumCostDrivers(List<CostDriverResponse> costDrivers) {
        return costDrivers.stream()
                .mapToDouble(CostDriverResponse::amount)
                .sum();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}