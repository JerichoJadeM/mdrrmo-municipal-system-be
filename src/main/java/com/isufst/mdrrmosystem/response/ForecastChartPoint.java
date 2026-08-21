package com.isufst.mdrrmosystem.response;

/**
 * Represents one point in the budget forecasting chart.
 *
 * year   = budget year
 * amount = actual or predictive budget amount for that year
 */
public record ForecastChartPoint(
        int year,
        double amount
) {
}
