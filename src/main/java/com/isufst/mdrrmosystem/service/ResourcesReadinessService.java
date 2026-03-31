package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.response.BudgetCurrentSummaryResponse;
import com.isufst.mdrrmosystem.response.ResourcesReadinessSummaryResponse;
import com.isufst.mdrrmosystem.response.ResourcesSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ResourcesReadinessService {

    private static final Set<String> RELIEF_CATEGORIES =
            Set.of("FOOD", "RELIEF", "WATER", "HYGIENE", "MEDICAL");

    private final InventoryRepository inventoryRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final BudgetService budgetService;

    public ResourcesReadinessService(InventoryRepository inventoryRepository,
                                     EvacuationActivationRepository evacuationActivationRepository,
                                     BudgetService budgetService) {
        this.inventoryRepository = inventoryRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.budgetService = budgetService;
    }

    @Transactional(readOnly = true)
    public ResourcesSummaryResponse getSummary() {
        List<Inventory> inventory = inventoryRepository.findAll();

        long lowStockInventoryCount = inventory.stream().filter(this::isLowStock).count();
        long reliefReadyCount = inventory.stream()
                .filter(this::isReliefItem)
                .filter(i -> !isOutOfStock(i))
                .count();
        long reliefLowStockCount = inventory.stream()
                .filter(this::isReliefItem)
                .filter(this::isLowStock)
                .count();

        BudgetCurrentSummaryResponse budget = budgetService.getCurrentSummary();

        List<EvacuationActivation> openCenters = evacuationActivationRepository.findByStatus("OPEN");
        int totalCapacity = openCenters.stream()
                .filter(a -> a.getCenter() != null)
                .mapToInt(a -> Math.max(a.getCenter().getCapacity(), 0))
                .sum();
        int totalEvacuees = openCenters.stream()
                .mapToInt(EvacuationActivation::getCurrentEvacuees)
                .sum();
        int occupancyRate = totalCapacity > 0
                ? Math.min(100, Math.round((totalEvacuees * 100f) / totalCapacity))
                : 0;

        return new ResourcesSummaryResponse(
                inventory.size(),
                lowStockInventoryCount,
                budget.totalRemaining(),
                budget.totalObligations(),
                reliefReadyCount,
                reliefLowStockCount,
                openCenters.size(),
                occupancyRate
        );
    }

    @Transactional(readOnly = true)
    public ResourcesReadinessSummaryResponse getReadinessSummary() {
        List<Inventory> inventory = inventoryRepository.findAll();

        long inventoryLowStockCount = inventory.stream().filter(this::isLowStock).count();
        long inventoryOutOfStockCount = inventory.stream().filter(this::isOutOfStock).count();

        long usableInventoryCount = inventory.stream()
                .filter(i -> i.getAvailableQuantity() > 0)
                .count();

        long criticalAvailableCount = inventory.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(i -> i.getAvailableQuantity() > 0)
                .count();

        long criticalLowOrOutCount = inventory.stream()
                .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()))
                .filter(i -> isLowStock(i) || isOutOfStock(i))
                .count();

        long distinctInventoryCategories = inventory.stream()
                .map(Inventory::getCategory)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .distinct()
                .count();

        List<Inventory> reliefInventory = inventory.stream().filter(this::isReliefItem).toList();
        long reliefLowStockCount = reliefInventory.stream().filter(this::isLowStock).count();
        long estimatedFamilyCoverage = reliefInventory.stream()
                .filter(i -> {
                    String category = normalize(i.getCategory());
                    return "FOOD".equals(category) || "RELIEF".equals(category);
                })
                .mapToLong(Inventory::getAvailableQuantity)
                .sum();

        List<EvacuationActivation> openCenters = evacuationActivationRepository.findByStatus("OPEN");
        long activeCentersCount = openCenters.size();
        long nearFullCentersCount = 0;
        long fullCentersCount = 0;
        int totalCapacity = 0;
        int totalEvacuees = 0;

        for (EvacuationActivation activation : openCenters) {
            if (activation.getCenter() == null) {
                continue;
            }

            int capacity = Math.max(activation.getCenter().getCapacity(), 0);
            int evacuees = Math.max(activation.getCurrentEvacuees(), 0);
            int occupancy = capacity > 0
                    ? Math.min(100, Math.round((evacuees * 100f) / capacity))
                    : 0;

            totalCapacity += capacity;
            totalEvacuees += evacuees;

            if (capacity > 0 && occupancy >= 95) {
                fullCentersCount++;
            } else if (capacity > 0 && occupancy >= 80) {
                nearFullCentersCount++;
            }
        }

        int overallOccupancyRate = totalCapacity > 0
                ? Math.min(100, Math.round((totalEvacuees * 100f) / totalCapacity))
                : 0;

        BudgetCurrentSummaryResponse budget = budgetService.getCurrentSummary();
        int budgetUtilizationRate = (int) Math.round(budget.utilizationRate());
        double forecastGap = budget.totalAllotment() * 0.10;

        String inventoryRiskLevel = deriveInventoryRisk(
                inventory,
                usableInventoryCount,
                distinctInventoryCategories,
                criticalAvailableCount,
                inventoryLowStockCount,
                inventoryOutOfStockCount,
                criticalLowOrOutCount
        );

        String reliefRiskLevel = deriveReliefRisk(reliefLowStockCount, estimatedFamilyCoverage);
        String evacuationRiskLevel = deriveEvacuationRisk(
                activeCentersCount,
                totalCapacity,
                fullCentersCount,
                nearFullCentersCount
        );
        String budgetRiskLevel = deriveBudgetRisk(budgetUtilizationRate, budget.totalRemaining());

        int overallReadinessScore = deriveOverallReadinessScore(
                inventoryRiskLevel,
                reliefRiskLevel,
                evacuationRiskLevel,
                budgetRiskLevel
        );
        String overallReadinessRiskLevel = deriveOverallReadinessRiskLevel(
                inventoryRiskLevel,
                reliefRiskLevel,
                evacuationRiskLevel,
                budgetRiskLevel,
                overallReadinessScore
        );

        List<String> warnings = new ArrayList<>();

        if (inventory.isEmpty() || usableInventoryCount == 0) {
            warnings.add("No usable inventory is currently available.");
        }
        if (distinctInventoryCategories < 4) {
            warnings.add("Inventory category coverage is too limited for broader emergency operations.");
        }
        if (criticalAvailableCount < 3) {
            warnings.add("Critical response inventory coverage is insufficient.");
        }
        if (inventoryLowStockCount > 0) {
            warnings.add("Some inventory items are low or out of stock.");
        }
        if (reliefLowStockCount > 0) {
            warnings.add("Relief-related inventory items need replenishment.");
        }
        if (activeCentersCount == 0 || totalCapacity <= 0) {
            warnings.add("No evacuation center capacity is currently available.");
        }
        if (nearFullCentersCount > 0) {
            warnings.add("One or more evacuation centers are near full.");
        }
        if (fullCentersCount > 0) {
            warnings.add("One or more evacuation centers are already full.");
        }
        if (budgetUtilizationRate >= 80) {
            warnings.add("Budget utilization is high and may affect response readiness.");
        }
        if (budget.totalRemaining() <= 0) {
            warnings.add("Remaining budget is already exhausted.");
        }

        return new ResourcesReadinessSummaryResponse(
                inventoryRiskLevel,
                inventoryLowStockCount,
                inventoryOutOfStockCount,
                reliefRiskLevel,
                reliefLowStockCount,
                estimatedFamilyCoverage,
                evacuationRiskLevel,
                activeCentersCount,
                nearFullCentersCount,
                fullCentersCount,
                overallOccupancyRate,
                budgetRiskLevel,
                budgetUtilizationRate,
                forecastGap,
                overallReadinessRiskLevel,
                overallReadinessScore,
                warnings
        );
    }

    private boolean isReliefItem(Inventory inventory) {
        return inventory.getCategory() != null
                && RELIEF_CATEGORIES.contains(inventory.getCategory().trim().toUpperCase(Locale.ROOT));
    }

    private boolean isOutOfStock(Inventory inventory) {
        return inventory.getAvailableQuantity() <= 0;
    }

    private boolean isLowStock(Inventory inventory) {
        int reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0;
        return inventory.getAvailableQuantity() <= reorderLevel;
    }

    private String deriveInventoryRisk(List<Inventory> inventory,
                                       long usableInventoryCount,
                                       long distinctInventoryCategories,
                                       long criticalAvailableCount,
                                       long inventoryLowStockCount,
                                       long inventoryOutOfStockCount,
                                       long criticalLowOrOutCount) {
        if (inventory == null || inventory.isEmpty() || usableInventoryCount == 0) {
            return "CRITICAL";
        }

        if (distinctInventoryCategories < 4 || criticalAvailableCount < 3 || usableInventoryCount < 5) {
            return "CRITICAL";
        }

        if (inventoryOutOfStockCount >= 3 || criticalLowOrOutCount >= 2) {
            return "CRITICAL";
        }

        if (inventoryLowStockCount >= 5 || inventoryOutOfStockCount >= 1) {
            return "HIGH";
        }

        if (inventoryLowStockCount >= 2) {
            return "MODERATE";
        }

        return "LOW";
    }

    private String deriveReliefRisk(long reliefLowStockCount, long estimatedFamilyCoverage) {
        if (estimatedFamilyCoverage < 20 || reliefLowStockCount >= 5) {
            return "CRITICAL";
        }
        if (estimatedFamilyCoverage < 50 || reliefLowStockCount >= 3) {
            return "HIGH";
        }
        if (reliefLowStockCount >= 1) {
            return "MODERATE";
        }
        return "LOW";
    }

    private String deriveEvacuationRisk(long activeCenters,
                                        int totalCapacity,
                                        long fullCenters,
                                        long nearFullCenters) {
        if (activeCenters <= 0 || totalCapacity <= 0) {
            return "CRITICAL";
        }
        if (fullCenters > 0) {
            return "CRITICAL";
        }
        if (nearFullCenters >= 2) {
            return "HIGH";
        }
        if (nearFullCenters == 1) {
            return "MODERATE";
        }
        return "LOW";
    }

    private String deriveBudgetRisk(int utilizationRate, double remainingBudget) {
        if (remainingBudget <= 0 || utilizationRate >= 95) {
            return "CRITICAL";
        }
        if (utilizationRate >= 80) {
            return "HIGH";
        }
        if (utilizationRate >= 50) {
            return "MODERATE";
        }
        return "LOW";
    }

    private int deriveOverallReadinessScore(String inventoryRiskLevel,
                                            String reliefRiskLevel,
                                            String evacuationRiskLevel,
                                            String budgetRiskLevel) {
        int score = 100;

        score -= riskPenalty(inventoryRiskLevel);
        score -= riskPenalty(reliefRiskLevel);
        score -= riskPenalty(evacuationRiskLevel);
        score -= riskPenalty(budgetRiskLevel);

        return Math.max(0, Math.min(100, score));
    }

    private String deriveOverallReadinessRiskLevel(String inventoryRiskLevel,
                                                   String reliefRiskLevel,
                                                   String evacuationRiskLevel,
                                                   String budgetRiskLevel,
                                                   int score) {
        if (isCritical(inventoryRiskLevel)
                || isCritical(evacuationRiskLevel)
                || isCritical(budgetRiskLevel)) {
            return "CRITICAL";
        }

        if (isHigh(inventoryRiskLevel)
                || isHigh(reliefRiskLevel)
                || isHigh(evacuationRiskLevel)
                || isHigh(budgetRiskLevel)) {
            return "HIGH";
        }

        if (score < 60) {
            return "HIGH";
        }
        if (score < 80) {
            return "MODERATE";
        }
        return "LOW";
    }

    private int riskPenalty(String level) {
        return switch (normalize(level)) {
            case "CRITICAL" -> 35;
            case "HIGH" -> 24;
            case "MODERATE" -> 12;
            default -> 5;
        };
    }

    private boolean isCritical(String level) {
        return "CRITICAL".equals(normalize(level));
    }

    private boolean isHigh(String level) {
        return "HIGH".equals(normalize(level));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}