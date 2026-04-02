package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.EvacuationActivation;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.repository.EvacuationActivationRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.InventoryTransactionRepository;
import com.isufst.mdrrmosystem.response.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ResourcesReadinessService {

    private static final Set<String> RELIEF_CATEGORIES = Set.of("FOOD", "RELIEF", "WATER", "HYGIENE", "MEDICAL");

    private final InventoryRepository inventoryRepository;
    private final EvacuationActivationRepository evacuationActivationRepository;
    private final BudgetService budgetService;
    private final EvacuationCenterService evacuationCenterService;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public ResourcesReadinessService(InventoryRepository inventoryRepository,
                                     EvacuationActivationRepository evacuationActivationRepository,
                                     BudgetService budgetService,
                                     EvacuationCenterService evacuationCenterService,
                                     InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.evacuationActivationRepository = evacuationActivationRepository;
        this.budgetService = budgetService;
        this.evacuationCenterService = evacuationCenterService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
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
        List<Inventory> inventories = inventoryRepository.findAll();

        List<Inventory> reliefItems = inventories.stream()
                .filter(item -> isReliefCategory(item.getCategory()))
                .toList();

        long inventoryLowStockCount = inventories.stream()
                .filter(this::isLowStock)
                .count();

        long inventoryOutOfStockCount = inventories.stream()
                .filter(item -> safeInt(item.getAvailableQuantity()) <= 0)
                .count();

        long reliefLowStockCount = reliefItems.stream()
                .filter(this::isLowStock)
                .count();

        long totalReliefAvailable = reliefItems.stream()
                .mapToLong(item -> Math.max(safeInt(item.getAvailableQuantity()), 0))
                .sum();

        long estimatedFamilyCoverage = Math.max(totalReliefAvailable / 10, 0);

        List<EvacuationCenterResourceResponse> centers =
                evacuationCenterService.getResourcesView(null, null, null);

        long activeCentersCount = centers.stream()
                .filter(center -> {
                    String status = normalize(center.status());
                    return "active".equals(status) || "open".equals(status);
                })
                .count();

        long nearFullCentersCount = centers.stream()
                .filter(center -> {
                    int occupancy = resolveOccupancyRate(center);
                    return occupancy >= 80 && occupancy < 95;
                })
                .count();

        long fullCentersCount = centers.stream()
                .filter(center -> resolveOccupancyRate(center) >= 95)
                .count();

        int totalCapacity = centers.stream()
                .mapToInt(center -> Math.max(resolveCapacity(center), 0))
                .sum();

        int totalEvacuees = centers.stream()
                .mapToInt(center -> Math.max(resolveCurrentEvacuees(center), 0))
                .sum();

        int overallOccupancyRate = totalCapacity > 0
                ? (int) Math.round((totalEvacuees * 100.0) / totalCapacity)
                : 0;

        BudgetCurrentSummaryResponse budgetCurrent = budgetService.getCurrentSummary();
        int budgetUtilizationRate = budgetCurrent != null && budgetCurrent.utilizationRate() != null
                ? (int) Math.round(budgetCurrent.utilizationRate())
                : 0;

        double forecastGap = Math.max((double) totalEvacuees - estimatedFamilyCoverage, 0);

        String inventoryRiskLevel = computeInventoryRiskLevel(
                inventoryLowStockCount,
                inventoryOutOfStockCount
        );

        String reliefRiskLevel = computeReliefRiskLevel(
                reliefLowStockCount,
                (int) estimatedFamilyCoverage,
                totalEvacuees
        );

        String evacuationRiskLevel = computeEvacuationRiskLevel(
                overallOccupancyRate,
                nearFullCentersCount,
                fullCentersCount
        );

        String budgetRiskLevel = computeBudgetRiskLevel(budgetUtilizationRate);

        List<String> warnings = buildReadinessWarnings(
                inventoryLowStockCount,
                inventoryOutOfStockCount,
                reliefLowStockCount,
                forecastGap,
                totalEvacuees,
                nearFullCentersCount,
                fullCentersCount,
                budgetUtilizationRate
        );

        int overallReadinessScore = computeOverallReadinessScore(
                inventoryLowStockCount,
                inventoryOutOfStockCount,
                reliefLowStockCount,
                (int) estimatedFamilyCoverage,
                totalEvacuees,
                overallOccupancyRate,
                nearFullCentersCount,
                fullCentersCount,
                budgetUtilizationRate
        );

        String overallReadinessRiskLevel = mapScoreToRiskLevel(overallReadinessScore);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        LocalDateTime toDate = LocalDateTime.now().plusSeconds(1);

        List<TopConsumedResourceResponse> topConsumedResources =
                inventoryTransactionRepository.findTopConsumedResources(
                        fromDate,
                        toDate,
                        org.springframework.data.domain.PageRequest.of(0, 8)
                );

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
                warnings,
                topConsumedResources
        );
    }

    private List<String> buildReadinessWarnings(long inventoryLowStockCount,
                                                long inventoryOutOfStockCount,
                                                long reliefLowStockCount,
                                                double forecastGap,
                                                int totalEvacuees,
                                                long nearFullCentersCount,
                                                long fullCentersCount,
                                                int budgetUtilizationRate) {
        List<String> warnings = new ArrayList<>();

        if (inventoryOutOfStockCount > 0) {
            warnings.add(inventoryOutOfStockCount + " inventory item(s) are out of stock.");
        }

        if (inventoryLowStockCount > 0) {
            warnings.add(inventoryLowStockCount + " inventory item(s) are at low stock.");
        }

        if (reliefLowStockCount > 0) {
            warnings.add(reliefLowStockCount + " relief item(s) are at low stock.");
        }

        if (fullCentersCount > 0) {
            warnings.add(fullCentersCount + " evacuation center(s) are already full.");
        } else if (nearFullCentersCount > 0) {
            warnings.add(nearFullCentersCount + " evacuation center(s) are nearing full capacity.");
        }

        if (budgetUtilizationRate >= 90) {
            warnings.add("Budget utilization is critical at " + budgetUtilizationRate + "%.");
        } else if (budgetUtilizationRate >= 75) {
            warnings.add("Budget utilization is high at " + budgetUtilizationRate + "%.");
        } else if (budgetUtilizationRate >= 50) {
            warnings.add("Budget utilization is elevated at " + budgetUtilizationRate + "%.");
        }

        if (forecastGap > 0) {
            int shortagePercent = totalEvacuees > 0
                    ? (int) Math.round((forecastGap * 100.0) / totalEvacuees)
                    : 0;

            warnings.add(
                    "Estimated relief coverage is short by "
                            + Math.round(forecastGap)
                            + " family-equivalent unit(s)"
                            + (totalEvacuees > 0 ? " (" + shortagePercent + "% of current evacuee load)." : ".")
            );
        }

        return warnings;
    }

    private String computeInventoryRiskLevel(long lowStockCount, long outOfStockCount) {
        if (outOfStockCount >= 3 || lowStockCount >= 10) return "CRITICAL";
        if (outOfStockCount >= 1 || lowStockCount >= 5) return "HIGH";
        if (lowStockCount >= 1) return "MODERATE";
        return "LOW";
    }

    private String computeReliefRiskLevel(long reliefLowStockCount,
                                          int estimatedFamilyCoverage,
                                          int totalEvacuees) {
        int coverageGap = Math.max(totalEvacuees - estimatedFamilyCoverage, 0);
        double shortageRatio = totalEvacuees > 0
                ? (coverageGap * 1.0) / totalEvacuees
                : 0.0;

        if ((coverageGap > 0 && shortageRatio >= 0.50)
                || coverageGap >= 100
                || (totalEvacuees > 0 && estimatedFamilyCoverage == 0)) {
            return "CRITICAL";
        }

        if (coverageGap > 0 || reliefLowStockCount >= 5 || estimatedFamilyCoverage <= 20) {
            return "HIGH";
        }

        if (reliefLowStockCount >= 1 || estimatedFamilyCoverage <= 50) {
            return "MODERATE";
        }

        return "LOW";
    }

    private String computeEvacuationRiskLevel(int overallOccupancyRate,
                                              long nearFullCentersCount,
                                              long fullCentersCount) {
        if (fullCentersCount > 0 || overallOccupancyRate >= 95) return "CRITICAL";
        if (nearFullCentersCount >= 2 || overallOccupancyRate >= 85) return "HIGH";
        if (nearFullCentersCount >= 1 || overallOccupancyRate >= 70) return "MODERATE";
        return "LOW";
    }

    private String computeBudgetRiskLevel(int budgetUtilizationRate) {
        if (budgetUtilizationRate >= 90) return "CRITICAL";
        if (budgetUtilizationRate >= 75) return "HIGH";
        if (budgetUtilizationRate >= 50) return "MODERATE";
        return "LOW";
    }

    private int computeOverallReadinessScore(long inventoryLowStockCount,
                                             long inventoryOutOfStockCount,
                                             long reliefLowStockCount,
                                             int estimatedFamilyCoverage,
                                             int totalEvacuees,
                                             int overallOccupancyRate,
                                             long nearFullCentersCount,
                                             long fullCentersCount,
                                             int budgetUtilizationRate) {
        int inventoryPenalty = clampPenalty(
                (int) (inventoryOutOfStockCount * 12 + inventoryLowStockCount * 3),
                35
        );

        int coverageGap = Math.max(totalEvacuees - estimatedFamilyCoverage, 0);
        double shortageRatio = totalEvacuees > 0
                ? (coverageGap * 1.0) / totalEvacuees
                : 0.0;

        int reliefPenalty = 0;
        if (coverageGap > 0) {
            reliefPenalty += 12;
            reliefPenalty += (int) Math.round(shortageRatio * 20);
        } else if (estimatedFamilyCoverage <= 20) {
            reliefPenalty += 14;
        } else if (estimatedFamilyCoverage <= 50) {
            reliefPenalty += 8;
        }
        reliefPenalty += (int) Math.min(reliefLowStockCount * 2, 10);
        reliefPenalty = clampPenalty(reliefPenalty, 35);

        int evacuationPenalty = 0;
        if (fullCentersCount > 0 || overallOccupancyRate >= 95) {
            evacuationPenalty = 30;
        } else if (nearFullCentersCount > 0 || overallOccupancyRate >= 70) {
            evacuationPenalty = 10
                    + (int) Math.min(nearFullCentersCount * 5, 10)
                    + Math.max(0, (overallOccupancyRate - 70) / 5);
        }
        evacuationPenalty = clampPenalty(evacuationPenalty, 25);

        int budgetPenalty;
        if (budgetUtilizationRate >= 90) {
            budgetPenalty = 20;
        } else if (budgetUtilizationRate >= 75) {
            budgetPenalty = 14;
        } else if (budgetUtilizationRate >= 50) {
            budgetPenalty = 8;
        } else {
            budgetPenalty = 0;
        }

        int totalPenalty = clampPenalty(
                inventoryPenalty + reliefPenalty + evacuationPenalty + budgetPenalty,
                90
        );

        return Math.max(10, 100 - totalPenalty);
    }

    private String mapScoreToRiskLevel(int score) {
        if (score <= 30) return "CRITICAL";
        if (score <= 55) return "HIGH";
        if (score <= 75) return "MODERATE";
        return "LOW";
    }

    private int clampPenalty(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private boolean isReliefCategory(String category) {
        String normalized = normalize(category);
        return normalized.contains("food")
                || normalized.contains("relief")
                || normalized.contains("water")
                || normalized.contains("medicine")
                || normalized.contains("medical")
                || normalized.contains("hygiene");
    }

    private boolean isLowStock(Inventory item) {
        int available = safeInt(item.getAvailableQuantity());
        int reorderLevel = item.getReorderLevel() != null ? item.getReorderLevel() : 0;
        return available > 0 && available <= reorderLevel;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private int resolveOccupancyRate(EvacuationCenterResourceResponse center) {
        if (center.occupancyRate() != null) {
            return Math.max(center.occupancyRate(), 0);
        }

        int capacity = resolveCapacity(center);
        int evacuees = resolveCurrentEvacuees(center);

        return capacity > 0
                ? (int) Math.round((evacuees * 100.0) / capacity)
                : 0;
    }

    private int resolveCapacity(EvacuationCenterResourceResponse center) {
        return center.capacity() != null ? center.capacity() : 0;
    }

    private int resolveCurrentEvacuees(EvacuationCenterResourceResponse center) {
        return center.currentEvacuees() != null ? center.currentEvacuees() : 0;
    }

    private String computeReliefRiskLevel(long reliefLowStockCount, int estimatedFamilyCoverage) {
        if (estimatedFamilyCoverage <= 20) return "HIGH";
        if (reliefLowStockCount >= 3 || estimatedFamilyCoverage <= 50) return "MODERATE";
        return "LOW";
    }

    private int riskLevelToScore(String riskLevel) {
        return switch (riskLevel) {
            case "LOW" -> 88;
            case "MODERATE" -> 66;
            case "HIGH" -> 40;
            default -> 60;
        };
    }

    private int computeOverallReadinessScore(String inventoryRiskLevel,
                                             String reliefRiskLevel,
                                             String evacuationRiskLevel,
                                             String budgetRiskLevel) {
        double average = (
                riskLevelToScore(inventoryRiskLevel)
                        + riskLevelToScore(reliefRiskLevel)
                        + riskLevelToScore(evacuationRiskLevel)
                        + riskLevelToScore(budgetRiskLevel)
        ) / 4.0;

        return (int) Math.round(average);
    }

    private boolean isReliefItem(Inventory inventory) {
        return inventory.getCategory() != null
                && RELIEF_CATEGORIES.contains(inventory.getCategory().trim().toUpperCase());
    }

    private boolean isOutOfStock(Inventory inventory) {
        return inventory.getAvailableQuantity() <= 0;
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