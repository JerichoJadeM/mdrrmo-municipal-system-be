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
import java.util.Set;

@Service
public class ResourcesReadinessService {

    private static final Set<String> RELIEF_CATEGORIES = Set.of("FOOD", "RELIEF", "WATER", "HYGIENE", "MEDICAL");

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
                .mapToInt(a -> a.getCenter().getCapacity())
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

        List<Inventory> reliefInventory = inventory.stream().filter(this::isReliefItem).toList();
        long reliefLowStockCount = reliefInventory.stream().filter(this::isLowStock).count();
        long estimatedFamilyCoverage = reliefInventory.stream()
                .filter(i -> "FOOD".equalsIgnoreCase(i.getCategory()) || "RELIEF".equalsIgnoreCase(i.getCategory()))
                .mapToLong(Inventory::getAvailableQuantity)
                .sum();

        List<EvacuationActivation> openCenters = evacuationActivationRepository.findByStatus("OPEN");
        long activeCentersCount = openCenters.size();
        long nearFullCentersCount = 0;
        long fullCentersCount = 0;
        int totalCapacity = 0;
        int totalEvacuees = 0;

        for (EvacuationActivation activation : openCenters) {
            if (activation.getCenter() == null) continue;

            int capacity = Math.max(activation.getCenter().getCapacity(), 0);
            int evacuees = activation.getCurrentEvacuees();
            int occupancy = capacity > 0
                    ? Math.min(100, Math.round((evacuees * 100f) / capacity))
                    : 0;

            totalCapacity += capacity;
            totalEvacuees += evacuees;

            if (occupancy >= 95) {
                fullCentersCount++;
            } else if (occupancy >= 80) {
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
                inventoryLowStockCount,
                inventoryOutOfStockCount,
                inventory.stream()
                        .filter(i -> Boolean.TRUE.equals(i.getCriticalItem()) && (isLowStock(i) || isOutOfStock(i)))
                        .count()
        );

        String reliefRiskLevel = deriveReliefRisk(reliefLowStockCount, estimatedFamilyCoverage);
        String evacuationRiskLevel = deriveEvacuationRisk(fullCentersCount, nearFullCentersCount, activeCentersCount);
        String budgetRiskLevel = deriveBudgetRisk(budgetUtilizationRate);

        int score = 0;
        score += riskPoints(inventoryRiskLevel);
        score += riskPoints(reliefRiskLevel);
        score += riskPoints(evacuationRiskLevel);
        score += riskPoints(budgetRiskLevel);

        int overallReadinessScore = Math.min(100, score);
        String overallReadinessRiskLevel = deriveOverall(overallReadinessScore);

        List<String> warnings = new ArrayList<>();
        if (inventoryLowStockCount > 0) warnings.add("Some inventory items are low or out of stock.");
        if (reliefLowStockCount > 0) warnings.add("Relief-related inventory items need replenishment.");
        if (nearFullCentersCount > 0) warnings.add("One or more evacuation centers are near full.");
        if (fullCentersCount > 0) warnings.add("One or more evacuation centers are already full.");
        if (budgetUtilizationRate >= 75) warnings.add("Budget utilization is high and may affect response readiness.");

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
                && RELIEF_CATEGORIES.contains(inventory.getCategory().trim().toUpperCase());
    }

    private boolean isOutOfStock(Inventory inventory) {
        return inventory.getAvailableQuantity() <= 0;
    }

    private boolean isLowStock(Inventory inventory) {
        int reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : 0;
        return inventory.getAvailableQuantity() <= reorderLevel;
    }

    private String deriveInventoryRisk(long lowCount, long outCount, long criticalLowCount) {
        if (outCount >= 3 || criticalLowCount >= 3) return "CRITICAL";
        if (lowCount >= 5 || outCount >= 1) return "HIGH";
        if (lowCount >= 2) return "MODERATE";
        return "LOW";
    }

    private String deriveReliefRisk(long reliefLowStockCount, long estimatedFamilyCoverage) {
        if (estimatedFamilyCoverage < 20 || reliefLowStockCount >= 5) return "CRITICAL";
        if (estimatedFamilyCoverage < 50 || reliefLowStockCount >= 3) return "HIGH";
        if (reliefLowStockCount >= 1) return "MODERATE";
        return "LOW";
    }

    private String deriveEvacuationRisk(long fullCenters, long nearFullCenters, long activeCenters) {
        if (fullCenters > 0) return "CRITICAL";
        if (nearFullCenters >= 2) return "HIGH";
        if (nearFullCenters == 1 || activeCenters > 0) return "MODERATE";
        return "LOW";
    }

    private String deriveBudgetRisk(int utilizationRate) {
        if (utilizationRate >= 90) return "CRITICAL";
        if (utilizationRate >= 75) return "HIGH";
        if (utilizationRate >= 50) return "MODERATE";
        return "LOW";
    }

    private int riskPoints(String level) {
        return switch (level) {
            case "CRITICAL" -> 30;
            case "HIGH" -> 22;
            case "MODERATE" -> 15;
            default -> 8;
        };
    }

    private String deriveOverall(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 65) return "HIGH";
        if (score >= 40) return "MODERATE";
        return "LOW";
    }
}