package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.response.BudgetForecastBreakdownResponse;
import com.isufst.mdrrmosystem.response.BudgetForecastResponse;
import com.isufst.mdrrmosystem.response.CategoryAllocationForecastResponse;
import com.isufst.mdrrmosystem.response.OperationTypeForecastResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BudgetForecastBreakdownService {

    private final BudgetForecastService budgetForecastService;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;
    private final ExpenseRepository expenseRepository;

    public BudgetForecastBreakdownService(BudgetForecastService budgetForecastService,
                                          IncidentRepository incidentRepository,
                                          CalamityRepository calamityRepository,
                                          ExpenseRepository expenseRepository) {
        this.budgetForecastService = budgetForecastService;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public BudgetForecastBreakdownResponse getNextYearForecastBreakdown() {
        BudgetForecastResponse official = budgetForecastService.getNextYearForecast();

        List<Incident> incidents = incidentRepository.findAll();
        List<Calamity> calamities = calamityRepository.findAll();

        double totalIncidentHistoricalCost = incidents.stream()
                .mapToDouble(incident -> safeAmount(expenseRepository.sumByIncidentId(incident.getId())))
                .sum();

        double totalCalamityHistoricalCost = calamities.stream()
                .mapToDouble(calamity -> safeAmount(expenseRepository.sumByCalamityId(calamity.getId())))
                .sum();

        double totalHistoricalOperationalCost = totalIncidentHistoricalCost + totalCalamityHistoricalCost;

        double incidentShare = totalHistoricalOperationalCost > 0
                ? totalIncidentHistoricalCost / totalHistoricalOperationalCost
                : 0.50;

        double calamityShare = totalHistoricalOperationalCost > 0
                ? totalCalamityHistoricalCost / totalHistoricalOperationalCost
                : 0.50;

        double incidentForecastTotal = official.totalForecast() * incidentShare;
        double calamityForecastTotal = official.totalForecast() * calamityShare;

        List<OperationTypeForecastResponse> incidentTypeForecasts =
                buildIncidentTypeForecasts(incidents, incidentForecastTotal);

        List<OperationTypeForecastResponse> calamityTypeForecasts =
                buildCalamityTypeForecasts(calamities, calamityForecastTotal);

        List<CategoryAllocationForecastResponse> categoryAllocationForecasts =
                official.categories().stream()
                        .map(row -> new CategoryAllocationForecastResponse(
                                row.section(),
                                row.category(),
                                row.finalAmount(),
                                row.historicalBaseline(),
                                row.trendAdjustment(),
                                row.ruleBasedAmount(),
                                row.historicalAdjustment(),
                                row.priceAdjustment(),
                                row.contingencyAmount(),
                                row.note()
                        ))
                        .toList();

        return new BudgetForecastBreakdownResponse(
                official.year(),
                official.totalForecast(),
                incidentForecastTotal,
                calamityForecastTotal,
                incidentShare * 100,
                calamityShare * 100,
                incidentTypeForecasts,
                calamityTypeForecasts,
                categoryAllocationForecasts
        );
    }

    private List<OperationTypeForecastResponse> buildIncidentTypeForecasts(List<Incident> incidents,
                                                                           double incidentForecastTotal) {
        Map<String, List<Incident>> grouped = incidents.stream()
                .collect(Collectors.groupingBy(incident -> safeLabel(incident.getType())));

        double totalHistoricalCost = grouped.values().stream()
                .flatMap(List::stream)
                .mapToDouble(incident -> safeAmount(expenseRepository.sumByIncidentId(incident.getId())))
                .sum();

        return grouped.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    List<Incident> rows = entry.getValue();

                    long historicalCount = rows.size();
                    double historicalCost = rows.stream()
                            .mapToDouble(incident -> safeAmount(expenseRepository.sumByIncidentId(incident.getId())))
                            .sum();

                    double historicalAverageCost = historicalCount > 0
                            ? historicalCost / historicalCount
                            : 0;

                    double share = totalHistoricalCost > 0
                            ? historicalCost / totalHistoricalCost
                            : (double) historicalCount / Math.max(incidents.size(), 1);

                    double forecastAmount = incidentForecastTotal * share;

                    return new OperationTypeForecastResponse(
                            "INCIDENT",
                            type,
                            historicalCount,
                            historicalCost,
                            historicalAverageCost,
                            forecastAmount,
                            share * 100,
                            "Distributed from incident forecast chunk using historical expense weight."
                    );
                })
                .sorted(Comparator.comparing(OperationTypeForecastResponse::forecastAmount).reversed())
                .toList();
    }

    private List<OperationTypeForecastResponse> buildCalamityTypeForecasts(List<Calamity> calamities,
                                                                           double calamityForecastTotal) {
        Map<String, List<Calamity>> grouped = calamities.stream()
                .collect(Collectors.groupingBy(calamity -> safeLabel(calamity.getType())));

        double totalHistoricalCost = grouped.values().stream()
                .flatMap(List::stream)
                .mapToDouble(calamity -> safeAmount(expenseRepository.sumByCalamityId(calamity.getId())))
                .sum();

        return grouped.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey();
                    List<Calamity> rows = entry.getValue();

                    long historicalCount = rows.size();
                    double historicalCost = rows.stream()
                            .mapToDouble(calamity -> safeAmount(expenseRepository.sumByCalamityId(calamity.getId())))
                            .sum();

                    double historicalAverageCost = historicalCount > 0
                            ? historicalCost / historicalCount
                            : 0;

                    double share = totalHistoricalCost > 0
                            ? historicalCost / totalHistoricalCost
                            : (double) historicalCount / Math.max(calamities.size(), 1);

                    double forecastAmount = calamityForecastTotal * share;

                    return new OperationTypeForecastResponse(
                            "CALAMITY",
                            type,
                            historicalCount,
                            historicalCost,
                            historicalAverageCost,
                            forecastAmount,
                            share * 100,
                            "Distributed from calamity forecast chunk using historical expense weight."
                    );
                })
                .sorted(Comparator.comparing(OperationTypeForecastResponse::forecastAmount).reversed())
                .toList();
    }

    private double safeAmount(Double value) {
        return value == null ? 0 : value;
    }

    private String safeLabel(String value) {
        return (value == null || value.isBlank()) ? "Unspecified" : value.trim();
    }
}
