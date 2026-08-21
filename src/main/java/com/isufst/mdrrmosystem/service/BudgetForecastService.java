package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.PreviousBudget;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.PreviousBudgetRepository;
import com.isufst.mdrrmosystem.response.BudgetForecastCategoryResponse;
import com.isufst.mdrrmosystem.response.BudgetForecastDriverResponse;
import com.isufst.mdrrmosystem.response.BudgetForecastResponse;
import com.isufst.mdrrmosystem.response.ForecastChartPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Official next-year budget forecasting service.
 *
 * Forecasting approach:
 *
 * 1. Five-year Moving Average
 * 2. Trend Adjustment
 * 3. Historical Obligation Adjustment
 * 4. MDRRMO Rule-Based Adjustment
 * 5. Inventory Price Adjustment
 * 6. 10% Contingency
 *
 * The Moving Average is the statistical baseline of the forecast.
 * The remaining components are domain-specific adjustments for
 * disaster-management budgeting.
 */
@Service
public class BudgetForecastService {

    /*
     * ================================================================
     * MOVING AVERAGE CONFIGURATION
     * ================================================================
     *
     * The system uses a 5-period Moving Average.
     *
     * Since the budget data is annual, the 5 periods represent
     * the previous 5 budget years.
     *
     * Example:
     *
     * 2022  2023  2024  2025  2026
     *   │     │     │     │     │
     *   └─────┴─────┴─────┴─────┘
     *             ↓
     *          / 5
     *             ↓
     *       2027 Forecast
     */
    private static final int MOVING_AVERAGE_WINDOW = 5;

    private final BudgetRepository budgetRepository;
    private final PreviousBudgetRepository previousBudgetRepository;
    private final ExpenseRepository expenseRepository;
    private final InventoryRepository inventoryRepository;

    public BudgetForecastService(BudgetRepository budgetRepository,
                                 PreviousBudgetRepository previousBudgetRepository,
                                 ExpenseRepository expenseRepository,
                                 InventoryRepository inventoryRepository) {
        this.budgetRepository = budgetRepository;
        this.previousBudgetRepository = previousBudgetRepository;
        this.expenseRepository = expenseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * ================================================================
     * NEXT-YEAR BUDGET FORECAST
     * ================================================================
     *
     * This is the main forecasting method.
     *
     * The forecast is NOT based on Moving Average alone.
     *
     * Moving Average provides the statistical baseline, while
     * MDRRMO-specific factors adjust that baseline.
     *
     * Final Forecast =
     *
     * Moving Average
     * + Trend Adjustment
     * + Historical Obligation Adjustment
     * + Rule-Based Adjustment
     * + Price Adjustment
     * + Contingency
     */
    @Transactional(readOnly = true)
    public BudgetForecastResponse getNextYearForecast() {

        int currentYear = LocalDate.now().getYear();
        int nextYear = currentYear + 1;

        /*
         * ============================================================
         * 1. DETERMINE THE FIVE-YEAR HISTORICAL WINDOW
         * ============================================================
         *
         * If the current year is 2026:
         *
         * startYear = 2022
         *
         * Historical window:
         * 2022, 2023, 2024, 2025, 2026
         *
         * These five actual values are used by the Moving Average.
         */
        int startYear = currentYear - (MOVING_AVERAGE_WINDOW - 1);

        /*
         * Get operational budgets.
         *
         * Repository already returns the budgets ordered by year.
         * We explicitly sort again below so that the Moving Average
         * always receives chronological data.
         */
        List<Budget> operationalBudgets =
                budgetRepository.findAllByOrderByYearAsc()
                        .stream()
                        .filter(b -> b.getYear() >= startYear
                                && b.getYear() <= currentYear)
                        .sorted(Comparator.comparing(Budget::getYear))
                        .toList();

        /*
         * Get previous/historical budget records.
         *
         * PreviousBudget contains historical allotment information
         * but does not necessarily contain BudgetCategory records.
         *
         * Therefore, previous budgets are used primarily for the
         * annual total budget history and not for category-level
         * historical allocations.
         */
        List<PreviousBudget> previousBudgets =
                previousBudgetRepository.findAllByOrderByYearAsc()
                        .stream()
                        .filter(b -> b.getYear() >= startYear
                                && b.getYear() <= currentYear)
                        .sorted(Comparator.comparing(PreviousBudget::getYear))
                        .toList();

        /*
         * ============================================================
         * 2. BUILD ACTUAL ANNUAL BUDGET VALUES
         * ============================================================
         *
         * This map represents the actual historical budget value
         * for each year.
         *
         * Example:
         *
         * 2022 -> 1,000,000
         * 2023 -> 1,100,000
         * 2024 -> 1,200,000
         * 2025 -> 1,300,000
         * 2026 -> 1,400,000
         *
         * These are the Y values in:
         *
         * F(t+1) =
         * [Y(t) + Y(t-1) + ... + Y(t-k+1)] / k
         */
        Map<Integer, Double> annualBudgetHistory = new TreeMap<>();

        /*
         * Operational budgets are preferred when the same year exists.
         */
        for (Budget budget : operationalBudgets) {
            annualBudgetHistory.put(
                    budget.getYear(),
                    safeDouble(budget.getTotalAmount())
            );
        }

        /*
         * Add PreviousBudget values only when an operational budget
         * does not already exist for the same year.
         *
         * This prevents the same year's budget from being counted twice.
         */
        for (PreviousBudget previousBudget : previousBudgets) {
            annualBudgetHistory.putIfAbsent(
                    previousBudget.getYear(),
                    safeDouble(previousBudget.getAllotment())
            );
        }

        if (annualBudgetHistory.isEmpty()) {
            throw new RuntimeException(
                    "No budget history found for the previous "
                            + MOVING_AVERAGE_WINDOW + " years."
            );
        }

        /*
         * ============================================================
         * 3. CALCULATE THE FIVE-YEAR MOVING AVERAGE
         * ============================================================
         *
         * IMPORTANT:
         *
         * This is the actual Moving Average implementation.
         *
         * Standard Moving Average formula:
         *
         *                 Y(t) + Y(t-1) + ... + Y(t-k+1)
         * F(t+1) =        -----------------------------------
         *                              k
         *
         * Where:
         *
         * F(t+1) = forecast for the next period
         * Y(t)   = actual value in the current period
         * k      = number of previous periods included
         *
         * For this system:
         *
         * k = 5
         *
         * Therefore:
         *
         * F(2027) =
         *
         * (Y2026 + Y2025 + Y2024 + Y2023 + Y2022) / 5
         *
         * SOURCE OF FORMULA:
         *
         * NIST Engineering Statistics Handbook,
         * "Single Moving Average"
         *
         * NIST defines the moving average as:
         *
         * M(t) = [X(t) + X(t-1) + ... + X(t-N+1)] / N
         *
         * Reference:
         * https://www.itl.nist.gov/div898/handbook/pmc/section4/pmc421.htm
         *
         * The formula is therefore a standard statistical
         * Moving Average formula, not a formula invented specifically
         * for this system.
         */
        List<Double> annualBudgetValues =
                new ArrayList<>(annualBudgetHistory.values());

        double movingAverageForecast =
                calculateMovingAverage(
                        annualBudgetValues,
                        MOVING_AVERAGE_WINDOW
                );

        /*
         * ============================================================
         * 4. GET CURRENT YEAR BUDGET
         * ============================================================
         */
        Budget currentBudget =
                budgetRepository.findFirstByYear(currentYear)
                        .orElseGet(() -> {

                            /*
                             * If the current year exists only in
                             * PreviousBudget, create a temporary
                             * object containing the annual total.
                             *
                             * This object is used only for forecasting
                             * and is NOT saved to the database.
                             */
                            Budget fallback = new Budget();
                            fallback.setYear(currentYear);
                            fallback.setTotalAmount(
                                    annualBudgetHistory.getOrDefault(
                                            currentYear,
                                            movingAverageForecast
                                    )
                            );

                            return fallback;
                        });

        /*
         * ============================================================
         * 5. BUILD CATEGORY HISTORY
         * ============================================================
         *
         * We intentionally use the real Budget entities here.
         *
         * The previous implementation created new Budget objects
         * containing only year and totalAmount. That could cause
         * BudgetCategory information to be lost.
         *
         * Category-level Moving Average therefore uses the actual
         * historical BudgetCategory records available in the
         * operational budgets.
         */
        Map<String, List<CategoryHistoryPoint>> groupedCategoryHistory =
                new HashMap<>();

        for (Budget budget : operationalBudgets) {

            if (budget.getCategories() == null) {
                continue;
            }

            for (BudgetCategory category : budget.getCategories()) {

                String key = key(
                        category.getSection(),
                        category.getName()
                );

                groupedCategoryHistory
                        .computeIfAbsent(
                                key,
                                ignored -> new ArrayList<>()
                        )
                        .add(
                                new CategoryHistoryPoint(
                                        budget.getYear(),
                                        safeDouble(
                                                category.getAllocatedAmount()
                                        ),
                                        category.getId()
                                )
                        );
            }
        }

        /*
         * Moving Average must move through time in the correct order.
         */
        groupedCategoryHistory.values()
                .forEach(history ->
                        history.sort(
                                Comparator.comparing(
                                        CategoryHistoryPoint::year
                                )
                        )
                );

        /*
         * ============================================================
         * 6. FORECAST EACH BUDGET CATEGORY
         * ============================================================
         */
        List<ForecastTemplateRow> templateRows =
                getWorkbookForecastTemplate();

        List<BudgetForecastCategoryResponse> rows =
                new ArrayList<>();

        double totalForecast = 0;

        for (ForecastTemplateRow templateRow : templateRows) {

            String key = key(
                    templateRow.section(),
                    templateRow.category()
            );

            List<CategoryHistoryPoint> categoryHistory =
                    groupedCategoryHistory.getOrDefault(
                            key,
                            List.of()
                    );

            /*
             * ========================================================
             * 6A. CATEGORY MOVING AVERAGE
             * ========================================================
             *
             * If historical category values are available, calculate
             * the Moving Average using those actual historical values.
             *
             * Example:
             *
             * Food and Water:
             *
             * 2022 = 100,000
             * 2023 = 110,000
             * 2024 = 120,000
             * 2025 = 130,000
             * 2026 = 140,000
             *
             * Moving Average:
             *
             * (100,000 + 110,000 + 120,000
             *  + 130,000 + 140,000) / 5
             *
             * = 120,000
             */
            double historicalBaseline;

            if (!categoryHistory.isEmpty()) {

                /*
                 * Extract actual historical category allocations
                 * in chronological order.
                 */
                List<Double> categoryValues =
                        categoryHistory.stream()
                                .map(CategoryHistoryPoint::amount)
                                .toList();

                /*
                 * REAL MOVING AVERAGE:
                 *
                 * The last k actual observations are used.
                 *
                 * k = MOVING_AVERAGE_WINDOW = 5
                 */
                historicalBaseline =
                        calculateMovingAverage(
                                categoryValues,
                                MOVING_AVERAGE_WINDOW
                        );

            } else {

                /*
                 * If a category has no historical category records,
                 * there is no valid category-level time series from
                 * which to calculate a category Moving Average.
                 *
                 * In this case, use the current category allocation
                 * as the fallback baseline.
                 *
                 * This prevents the system from inventing historical
                 * category values.
                 */
                historicalBaseline =
                        safeDouble(
                                findCurrentCategoryAmount(
                                        currentBudget,
                                        templateRow.section(),
                                        templateRow.category()
                                )
                        );

                /*
                 * If the current category does not exist either,
                 * use the annual Moving Average multiplied by the
                 * workbook-defined category weight.
                 *
                 * This keeps the forecast aligned with the existing
                 * workbook category template.
                 */
                if (historicalBaseline <= 0) {
                    historicalBaseline =
                            movingAverageForecast
                                    * templateRow.defaultWeight();
                }
            }

            /*
             * ========================================================
             * 6B. TREND ADJUSTMENT
             * ========================================================
             *
             * Moving Average provides the baseline.
             *
             * This additional calculation adjusts the baseline based
             * on the change between the oldest and latest historical
             * category allocation.
             *
             * Trend Rate:
             *
             * (Latest - Oldest) / Oldest
             *
             * Trend Adjustment:
             *
             * Trend Rate × Moving Average Baseline
             *
             * This is NOT part of the Moving Average formula.
             * It is a separate MDRRMO forecasting adjustment.
             */
            double oldestAllocation =
                    categoryHistory.isEmpty()
                            ? historicalBaseline
                            : categoryHistory.get(0).amount();

            double latestAllocation =
                    categoryHistory.isEmpty()
                            ? historicalBaseline
                            : categoryHistory
                            .get(categoryHistory.size() - 1)
                            .amount();

            double trendAdjustment =
                    oldestAllocation > 0
                            ? ((latestAllocation - oldestAllocation)
                            / oldestAllocation)
                            * historicalBaseline
                            : 0;

            /*
             * ========================================================
             * 6C. HISTORICAL OBLIGATION ADJUSTMENT
             * ========================================================
             *
             * Historical obligations represent actual spending
             * recorded against the category.
             *
             * We calculate the average historical obligation and
             * add 20% of that value to the forecast.
             *
             * This is an additional adjustment.
             * It is NOT part of the Moving Average formula.
             */
            double historicalObligationAverage =
                    categoryHistory.stream()
                            .mapToDouble(history ->
                                    expenseRepository.sumByCategoryId(
                                            history.categoryId()
                                    )
                            )
                            .average()
                            .orElse(0);

            double historicalAdjustment =
                    historicalObligationAverage * 0.20;

            /*
             * ========================================================
             * 6D. MDRRMO RULE-BASED ADJUSTMENT
             * ========================================================
             *
             * This is where the system applies disaster-management
             * domain rules.
             *
             * Examples:
             *
             * Food/Water       -> +15%
             * Medical          -> +12%
             * Evacuation       -> +18%
             * Relief           -> +14%
             *
             * The Moving Average remains the statistical baseline.
             * These rules adjust the baseline based on MDRRMO needs.
             */
            double ruleBasedAmount =
                    estimateRuleBasedAmount(
                            templateRow.section(),
                            templateRow.category(),
                            historicalBaseline
                    );

            /*
             * ========================================================
             * 6E. PRICE ADJUSTMENT
             * ========================================================
             *
             * Inventory price signals are used only when there is
             * recent procurement activity within the same 5-year
             * historical forecasting window.
             *
             * This prevents old/non-recurring assets such as vehicles,
             * shelters, tools, and other durable equipment from
             * artificially inflating the forecast.
             *
             * Example:
             *
             * Current Year = 2026
             * Moving Average Window = 5 years
             *
             * Eligible procurement period:
             *
             * 2022-01-01 -> 2026-12-31
             */
            double priceAdjustment =
                    estimatePriceAdjustment(
                            templateRow.section(),
                            templateRow.category(),
                            startYear,
                            currentYear
                    );

            /*
             * ========================================================
             * 6F. CONTINGENCY
             * ========================================================
             *
             * A fixed 10% contingency is applied to the combined
             * forecast components.
             *
             * This is an uncertainty allowance and is separate
             * from the Moving Average.
             */
            double contingencyBase =
                    historicalBaseline
                            + trendAdjustment
                            + historicalAdjustment
                            + ruleBasedAmount
                            + priceAdjustment;

            double contingency =
                    contingencyBase * 0.10;

            /*
             * ========================================================
             * 6G. FINAL CATEGORY FORECAST
             * ========================================================
             *
             * Final Forecast =
             *
             * Moving Average
             * + Trend
             * + Historical Obligations
             * + MDRRMO Rules
             * + Price Signal
             * + Contingency
             */
            double finalAmount =
                    historicalBaseline
                            + trendAdjustment
                            + historicalAdjustment
                            + ruleBasedAmount
                            + priceAdjustment
                            + contingency;

            /*
             * Do not allow negative budget forecasts.
             */
            if (finalAmount < 0) {
                finalAmount = 0;
            }

            totalForecast += finalAmount;

            /*
             * Add the forecast result to the response.
             */
            rows.add(
                    new BudgetForecastCategoryResponse(
                            templateRow.section(),
                            templateRow.category(),
                            historicalBaseline,
                            trendAdjustment,
                            ruleBasedAmount,
                            historicalAdjustment,
                            priceAdjustment,
                            contingency,
                            finalAmount,

                            /*
                             * Explanation returned to the frontend.
                             *
                             * This also makes it clear that Moving
                             * Average is the baseline, while the
                             * remaining components are adjustments.
                             */
                            "5-year Moving Average baseline + trend "
                                    + "+ historical obligations "
                                    + "+ MDRRMO rule-based adjustment "
                                    + "+ price signal "
                                    + "+ 10% contingency."
                    )
            );
        }

        /*
         * ================================================================
         * 7. FORECASTING DRIVERS
         * ================================================================
         *
         * These descriptions can be displayed in the frontend,
         * reports, or thesis screenshots.
         */
        List<BudgetForecastDriverResponse> drivers =
                List.of(

                        new BudgetForecastDriverResponse(
                                "Moving Average",
                                MOVING_AVERAGE_WINDOW + " years",
                                "The next-year baseline is calculated "
                                        + "using the standard Moving Average "
                                        + "formula: the sum of the latest "
                                        + MOVING_AVERAGE_WINDOW
                                        + " actual annual values divided by "
                                        + MOVING_AVERAGE_WINDOW
                                        + "."
                        ),

                        new BudgetForecastDriverResponse(
                                "Historical Budget Window",
                                startYear + " - " + currentYear,
                                "Actual budget values from the previous "
                                        + MOVING_AVERAGE_WINDOW
                                        + " years are used as the historical "
                                        + "forecast basis."
                        ),

                        new BudgetForecastDriverResponse(
                                "Historical Obligations",
                                "Included",
                                "Actual expenses from historical budget "
                                        + "categories influence the forecast "
                                        + "through an obligation adjustment."
                        ),

                        new BudgetForecastDriverResponse(
                                "Rule-Based Disaster Pressure",
                                "Included",
                                "MDRRMO-specific rules adjust the Moving "
                                        + "Average baseline for food, water, "
                                        + "medical, evacuation, relief, "
                                        + "equipment, training, and other "
                                        + "disaster-management requirements."
                        ),

                        new BudgetForecastDriverResponse(
                                "Recent Procurement Price Signal",
                                "Included when applicable",
                                "Estimated unit costs from recent recurring inventory procurements "
                                        + "within the five-year historical window "
                                        + "are used as a price signal."
                        ),

                        new BudgetForecastDriverResponse(
                                "Contingency",
                                "10%",
                                "A 10% contingency allowance is applied "
                                        + "to the forecast components to "
                                        + "account for uncertainty."
                        )
                );

        // ================================================================
        // FORECAST CHART DATA
        // ================================================================
        //
        // ACTUAL SERIES:
        // Contains the actual historical annual budget values used by the
        // Moving Average calculation.
        //
        // PREDICTIVE SERIES:
        // Contains the current year's actual budget as the starting point,
        // followed by the next year's predicted budget.
        //
        // ================================================================

                List<ForecastChartPoint> actualSeries =
                        annualBudgetHistory.entrySet()
                                .stream()
                                .map(entry -> new ForecastChartPoint(
                                        entry.getKey(),
                                        entry.getValue()
                                ))
                                .toList();

                List<ForecastChartPoint> predictiveSeries =
                        new ArrayList<>();

        // Current year = last actual point.
        // This connects the actual line to the predictive line.
                predictiveSeries.add(
                        new ForecastChartPoint(
                                currentYear,
                                annualBudgetHistory.getOrDefault(
                                        currentYear,
                                        currentBudget.getTotalAmount()
                                )
                        )
                );

        // Next year = Moving Average + MDRRMO adjustments.
                predictiveSeries.add(
                        new ForecastChartPoint(
                                nextYear,
                                totalForecast
                        )
                );

        /*
         * ================================================================
         * 8. RETURN OFFICIAL FORECAST
         * ================================================================
         */
        return new BudgetForecastResponse(
                nextYear,
                totalForecast,
                "Hybrid MDRRMO budget forecast using a "
                        + MOVING_AVERAGE_WINDOW
                        + "-year Moving Average as the statistical baseline, "
                        + "combined with historical obligations, "
                        + "disaster-management rules, price signals, "
                        + "and contingency.",
                drivers,
                rows.stream()
                        .sorted(
                                Comparator.comparing(
                                        BudgetForecastCategoryResponse::section
                                ).thenComparing(
                                        BudgetForecastCategoryResponse::category
                                )
                        )
                        .toList(),

                // Historical actual budget values
                actualSeries,

                // Current actual + next - year predictive forecast
                predictiveSeries
        );
    }

    /**
     * ================================================================
     * SIMPLE / SINGLE MOVING AVERAGE IMPLEMENTATION
     * ================================================================
     *
     * This method directly implements the required Moving Average
     * formula.
     *
     * Standard formula:
     *
     *              Y(t) + Y(t-1) + ... + Y(t-k+1)
     * F(t+1) =     --------------------------------
     *                         k
     *
     * For this system:
     *
     * k = 5
     *
     * Therefore:
     *
     * F(t+1) =
     * [Y(t) + Y(t-1) + Y(t-2) + Y(t-3) + Y(t-4)] / 5
     *
     * Example:
     *
     * 2022 = 1,000,000
     * 2023 = 1,100,000
     * 2024 = 1,200,000
     * 2025 = 1,300,000
     * 2026 = 1,400,000
     *
     * Forecast:
     *
     * (1,000,000 + 1,100,000 + 1,200,000
     *  + 1,300,000 + 1,400,000) / 5
     *
     * = 1,200,000
     *
     * WHY IS IT CALLED "MOVING"?
     *
     * The window moves forward whenever a new actual period becomes
     * available.
     *
     * Example:
     *
     * Forecast 2027:
     * 2022 2023 2024 2025 2026
     *
     * Forecast 2028:
     *       2023 2024 2025 2026 2027
     *
     * Forecast 2029:
     *             2024 2025 2026 2027 2028
     *
     * The oldest observation is removed and the newest observation
     * enters the window.
     *
     * SOURCE:
     *
     * NIST Engineering Statistics Handbook,
     * "Single Moving Average"
     *
     * NIST formula:
     *
     * M(t) = [X(t) + X(t-1) + ... + X(t-N+1)] / N
     *
     * Reference:
     * https://www.itl.nist.gov/div898/handbook/pmc/section4/pmc421.htm
     *
     * IMPORTANT:
     *
     * This is a Simple/Single Moving Average because every observation
     * inside the window has equal weight:
     *
     * 5-year window:
     *
     * Y(t)     -> 1/5 = 20%
     * Y(t-1)   -> 1/5 = 20%
     * Y(t-2)   -> 1/5 = 20%
     * Y(t-3)   -> 1/5 = 20%
     * Y(t-4)   -> 1/5 = 20%
     *
     * The system therefore does NOT use Weighted Moving Average or
     * Exponential Moving Average.
     */
    private double calculateMovingAverage(
            List<Double> actualValues,
            int windowSize) {

        if (actualValues == null || actualValues.isEmpty()) {
            return 0;
        }

        /*
         * A Moving Average requires the latest k observations.
         *
         * If there are more values than the selected window size,
         * remove the older values and keep only the latest k values.
         *
         * Example:
         *
         * Values:
         * [2019, 2020, 2021, 2022, 2023, 2024]
         *
         * k = 5
         *
         * Used:
         * [2020, 2021, 2022, 2023, 2024]
         */
        int startIndex =
                Math.max(
                        0,
                        actualValues.size() - windowSize
                );

        List<Double> movingWindow =
                actualValues.subList(
                        startIndex,
                        actualValues.size()
                );

        /*
         * Calculate:
         *
         * Sum of the latest k actual values
         * ---------------------------------
         *                  k
         *
         * This is the mathematical Moving Average formula.
         */
        return movingWindow.stream()
                .mapToDouble(value ->
                        value == null ? 0 : value
                )
                .average()
                .orElse(0);
    }

    /**
     * Find the current year's category allocation.
     *
     * This is used only when there is no historical category record.
     */
    private double findCurrentCategoryAmount(
            Budget currentBudget,
            String section,
            String categoryName) {

        if (currentBudget == null
                || currentBudget.getCategories() == null) {
            return 0;
        }

        return currentBudget.getCategories()
                .stream()
                .filter(category ->
                        key(
                                category.getSection(),
                                category.getName()
                        ).equals(
                                key(section, categoryName)
                        )
                )
                .mapToDouble(category ->
                        safeDouble(
                                category.getAllocatedAmount()
                        )
                )
                .findFirst()
                .orElse(0);
    }

    /**
     * ================================================================
     * MDRRMO RULE-BASED ADJUSTMENT
     * ================================================================
     *
     * These rules are NOT the Moving Average.
     *
     * They are domain-specific adjustments applied AFTER the
     * statistical Moving Average baseline has been calculated.
     */
    private double estimateRuleBasedAmount(
            String section,
            String category,
            double historicalBaseline) {

        String safeSection = safe(section);
        String safeCategory = safe(category);

        /*
         * Disaster Response
         */
        if ("DISASTER RESPONSE".equals(safeSection)) {

            if (safeCategory.contains("FOOD")
                    || safeCategory.contains("WATER")) {
                return historicalBaseline * 0.15;
            }

            if (safeCategory.contains("MEDICAL")) {
                return historicalBaseline * 0.12;
            }

            if (safeCategory.contains("EVAC")) {
                return historicalBaseline * 0.18;
            }

            if (safeCategory.contains("DRUG")) {
                return historicalBaseline * 0.10;
            }

            return historicalBaseline * 0.10;
        }

        /*
         * Disaster Preparedness
         */
        if ("DISASTER PREPAREDNESS".equals(safeSection)) {

            if (safeCategory.contains("EQUIPMENT")) {
                return historicalBaseline * 0.10;
            }

            if (safeCategory.contains("TRAINING")) {
                return historicalBaseline * 0.06;
            }

            if (safeCategory.contains("TRAVEL")) {
                return historicalBaseline * 0.04;
            }

            return historicalBaseline * 0.05;
        }

        /*
         * Disaster Prevention and Mitigation
         */
        if ("DISASTER PREVENTION AND MITIGATION"
                .equals(safeSection)) {

            if (safeCategory.contains("CAPITAL")) {
                return historicalBaseline * 0.08;
            }

            return historicalBaseline * 0.05;
        }

        /*
         * Disaster Rehabilitation and Recovery
         */
        if ("DISASTER REHABILITATION AND RECOVERY"
                .equals(safeSection)) {

            return historicalBaseline * 0.06;
        }

        /*
         * Default MDRRMO adjustment.
         */
        return historicalBaseline * 0.05;
    }

    /**
     * ================================================================
     * INVENTORY PRICE ADJUSTMENT
     * ================================================================
     *
     * Uses the average estimated unit cost from inventory as a
     * procurement price signal.
     *
     * This is NOT part of the Moving Average.
     */
    /**
     * ================================================================
     * INVENTORY PRICE ADJUSTMENT
     * ================================================================
     *
     * Inventory estimated unit costs are used as a RECENT PRICE SIGNAL.
     *
     * IMPORTANT:
     *
     * The inventory table may contain assets that were procured many
     * years ago. Those old procurement costs should NOT automatically
     * influence the next-year budget forecast.
     *
     * Therefore, only inventory items whose procurement expense date
     * falls inside the same historical window used by the Moving Average
     * are considered.
     *
     * Example:
     *
     * Current Year = 2026
     * Moving Average Window = 5 years
     *
     * Eligible procurement period:
     *
     * 2022-01-01
     *       through
     * 2026-12-31
     *
     * ================================================================
     *
     * NON-RECURRING INVENTORY
     * ================================================================
     *
     * The following inventory categories are excluded from the price
     * adjustment:
     *
     * VEHICLE
     * SHELTER
     * TOOL
     * OTHER
     *
     * These items are generally durable/non-recurring assets.
     *
     * Example:
     *
     * A vehicle purchased for ₱10,000,000 several years ago should not
     * cause the 2027 budget forecast to suddenly increase by millions
     * simply because that vehicle remains in inventory.
     *
     * ================================================================
     *
     * RECURRING INVENTORY
     * ================================================================
     *
     * Recent procurement of consumable or recurring items can still
     * provide a useful price signal.
     *
     * Examples:
     *
     * CONSUMABLE
     * SUPPLY
     * PPE
     * MEDICINE
     * FOOD
     * WATER
     *
     * ================================================================
     *
     * This price adjustment is NOT part of the Moving Average formula.
     * It is a separate domain-specific adjustment.
     */
    private double estimatePriceAdjustment(
            String section,
            String category,
            int startYear,
            int currentYear) {

        /*
         * ============================================================
         * 1. DEFINE THE HISTORICAL PROCUREMENT WINDOW
         * ============================================================
         *
         * The procurement window must match the Moving Average window.
         *
         * Example:
         *
         * 5-year Moving Average:
         *
         * 2022
         * 2023
         * 2024
         * 2025
         * 2026
         *
         * Therefore only procurement expenses from those years
         * are considered.
         */
        LocalDate startDate =
                LocalDate.of(
                        startYear,
                        1,
                        1
                );

        LocalDate endDate =
                LocalDate.of(
                        currentYear,
                        12,
                        31
                );

        /*
         * ============================================================
         * 2. INVENTORY CATEGORIES THAT SHOULD NOT AFFECT PRICE SIGNAL
         * ============================================================
         *
         * These are generally durable/non-recurring inventory.
         *
         * A vehicle or major equipment purchase can be extremely
         * expensive compared with normal recurring supplies.
         *
         * Including them in a simple average could therefore distort
         * the price signal.
         */
        Set<String> nonRecurringCategories =
                Set.of(
                        "VEHICLE",
                        "SHELTER",
                        "TOOL",
                        "OTHER"
                );

        /*
         * ============================================================
         * 3. GET INVENTORY
         * ============================================================
         */
        List<Inventory> inventory =
                inventoryRepository.findAll();

        /*
         * ============================================================
         * 4. FILTER INVENTORY
         * ============================================================
         *
         * An inventory item is considered a valid price signal only if:
         *
         * 1. It has an estimated unit cost.
         * 2. The estimated unit cost is greater than zero.
         * 3. It belongs to a recurring inventory category.
         * 4. It has a procurement expense.
         * 5. The procurement expense has a valid date.
         * 6. The procurement date falls inside the 5-year window.
         */
        List<Inventory> eligibleInventory =
                inventory.stream()
                        .filter(i ->
                                i.getEstimatedUnitCost() != null
                                        && i.getEstimatedUnitCost() > 0
                        )
                        .filter(i -> {

                            /*
                             * Normalize inventory category.
                             */
                            String inventoryCategory =
                                    i.getCategory() == null
                                            ? ""
                                            : i.getCategory()
                                            .trim()
                                            .toUpperCase();

                            /*
                             * Exclude non-recurring/durable inventory.
                             */
                            return !nonRecurringCategories
                                    .contains(inventoryCategory);
                        })
                        .filter(i -> {

                            /*
                             * Inventory must be linked to the actual
                             * procurement expense.
                             */
                            if (i.getProcurementExpense() == null) {
                                return false;
                            }

                            /*
                             * Procurement expense must have a date.
                             */
                            LocalDate procurementDate =
                                    i.getProcurementExpense()
                                            .getExpenseDate();

                            if (procurementDate == null) {
                                return false;
                            }

                            /*
                             * Only procurement within the historical
                             * forecasting window is considered.
                             *
                             * Example:
                             *
                             * 2022-01-01 <= procurementDate <= 2026-12-31
                             */
                            return !procurementDate.isBefore(startDate)
                                    && !procurementDate.isAfter(endDate);
                        })
                        .toList();

        /*
         * ============================================================
         * 5. NO VALID PRICE SIGNAL
         * ============================================================
         *
         * If no eligible inventory exists, the price adjustment should
         * simply be zero.
         *
         * This is important because the absence of recent procurement
         * should NOT create an artificial price adjustment.
         */
        if (eligibleInventory.isEmpty()) {
            return 0;
        }

        /*
         * ============================================================
         * 6. CALCULATE RECENT AVERAGE UNIT COST
         * ============================================================
         *
         * Only recent, recurring procurement is included.
         *
         * Old vehicles, old tools, old shelters, and old miscellaneous
         * assets have already been removed by the filters above.
         */
        double averageCost =
                eligibleInventory.stream()
                        .mapToDouble(
                                Inventory::getEstimatedUnitCost
                        )
                        .average()
                        .orElse(0);

        /*
         * Safety check.
         */
        if (averageCost <= 0) {
            return 0;
        }

        /*
         * ============================================================
         * 7. APPLY MDRRMO CATEGORY-SPECIFIC PRICE SIGNAL
         * ============================================================
         *
         * The average recent procurement cost is used only as a
         * price signal.
         *
         * It is NOT directly added as a complete budget amount.
         *
         * Different MDRRMO categories receive different multipliers
         * according to their expected procurement pressure.
         */
        String safeSection =
                safe(section);

        String safeCategory =
                safe(category);

        /*
         * DISASTER RESPONSE
         */
        if ("DISASTER RESPONSE".equals(safeSection)) {

            /*
             * Food and water are recurring emergency supplies.
             */
            if (safeCategory.contains("FOOD")
                    || safeCategory.contains("WATER")) {

                return averageCost * 25;
            }

            /*
             * Medical supplies are recurring emergency supplies.
             */
            if (safeCategory.contains("MEDICAL")) {

                return averageCost * 18;
            }

            /*
             * Evacuation support may require recurring supplies.
             */
            if (safeCategory.contains("EVAC")) {

                return averageCost * 15;
            }

            /*
             * Drugs and medicines are recurring.
             */
            if (safeCategory.contains("DRUG")) {

                return averageCost * 12;
            }

            /*
             * Other disaster-response operating expenses.
             */
            return averageCost * 10;
        }

        /*
         * DISASTER PREPAREDNESS
         */
        if ("DISASTER PREPAREDNESS".equals(safeSection)) {

            /*
             * Equipment is handled cautiously.
             * Old VEHICLE/TOOL/OTHER inventory has already been
             * excluded above.
             *
             * Only recent recurring inventory contributes here.
             */
            if (safeCategory.contains("EQUIPMENT")) {

                return averageCost * 8;
            }

            if (safeCategory.contains("TRAINING")) {

                return averageCost * 5;
            }

            if (safeCategory.contains("TRAVEL")) {

                return averageCost * 3;
            }

            return averageCost * 5;
        }

        /*
         * DISASTER PREVENTION AND MITIGATION
         */
        if ("DISASTER PREVENTION AND MITIGATION"
                .equals(safeSection)) {

            /*
             * Capital Outlay is generally non-recurring.
             *
             * Therefore, do NOT apply inventory price adjustment
             * to Capital Outlay.
             */
            if (safeCategory.contains("CAPITAL")) {

                return 0;
            }

            return averageCost * 5;
        }

        /*
         * DISASTER REHABILITATION AND RECOVERY
         */
        if ("DISASTER REHABILITATION AND RECOVERY"
                .equals(safeSection)) {

            return averageCost * 5;
        }

        /*
         * Default:
         *
         * Apply only a small price signal.
         */
        return averageCost * 3;
    }

    /**
     * Creates a consistent key for matching categories across years.
     */
    private String key(
            String section,
            String category) {

        return safe(section)
                + "|"
                + safe(category);
    }

    /**
     * Prevents null numeric values from causing calculation errors.
     */
    private double safeDouble(Double value) {
        return value == null ? 0 : value;
    }

    /**
     * Converts null strings into an empty string and normalizes
     * the value for reliable category matching.
     */
    private String safe(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase();
    }

    /**
     * Represents one historical category observation.
     *
     * Example:
     *
     * year       = 2026
     * amount     = 140000
     * categoryId = 25
     *
     * The year is important because Moving Average is a time-series
     * calculation and values must be processed chronologically.
     */
    private record CategoryHistoryPoint(
            int year,
            double amount,
            long categoryId
    ) {
    }

    /**
     * Workbook-aligned forecast category.
     *
     * defaultWeight is retained from the original implementation
     * and is only used as a fallback when a category has no historical
     * allocation and no current allocation.
     */
    private record ForecastTemplateRow(
            String section,
            String category,
            double defaultWeight
    ) {
    }

    /**
     * ================================================================
     * MDRRMO FORECAST TEMPLATE
     * ================================================================
     */
    private List<ForecastTemplateRow> getWorkbookForecastTemplate() {

        return List.of(

                new ForecastTemplateRow(
                        "DISASTER PREPAREDNESS",
                        "Training Expenses",
                        0.08
                ),

                new ForecastTemplateRow(
                        "DISASTER PREPAREDNESS",
                        "Traveling Expenses",
                        0.04
                ),

                new ForecastTemplateRow(
                        "DISASTER PREPAREDNESS",
                        "Rescue Equipment",
                        0.07
                ),

                new ForecastTemplateRow(
                        "DISASTER PREVENTION AND MITIGATION",
                        "Other Supplies and Materials",
                        0.08
                ),

                new ForecastTemplateRow(
                        "DISASTER PREVENTION AND MITIGATION",
                        "Capital Outlay",
                        0.10
                ),

                new ForecastTemplateRow(
                        "DISASTER RESPONSE",
                        "Food and Water",
                        0.14
                ),

                new ForecastTemplateRow(
                        "DISASTER RESPONSE",
                        "Medical Supplies",
                        0.08
                ),

                new ForecastTemplateRow(
                        "DISASTER RESPONSE",
                        "Drugs and Medicines Expenses",
                        0.07
                ),

                new ForecastTemplateRow(
                        "DISASTER RESPONSE",
                        "Evacuation Support",
                        0.08
                ),

                new ForecastTemplateRow(
                        "DISASTER RESPONSE",
                        "Maintenance and Other Operating Expenses",
                        0.10
                ),

                new ForecastTemplateRow(
                        "DISASTER REHABILITATION AND RECOVERY",
                        "Subsidy to Other Funds",
                        0.08
                ),

                new ForecastTemplateRow(
                        "DISASTER REHABILITATION AND RECOVERY",
                        "Other Supplies and Materials",
                        0.08
                )
        );
    }
}