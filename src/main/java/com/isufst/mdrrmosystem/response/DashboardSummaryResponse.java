package com.isufst.mdrrmosystem.response;

import java.util.List;

public class DashboardSummaryResponse {

    private final double totalBudget;
    private final double totalSpent;
    private final double remaining;
    private final long categoryCount;
    private final long expenseCount;
    private final List<CategoryBreakdownResponse> categoryBreakdown;
    private final long calamityCount;
    private final long activeIncidents;

    public DashboardSummaryResponse(double totalBudget,
                                    double totalSpent,
                                    double remaining,
                                    long categoryCount,
                                    long expenseCount,
                                    List<CategoryBreakdownResponse> categoryBreakdown,
                                    long calamityCount,
                                    long activeIncidents) {
        this.totalBudget = totalBudget;
        this.totalSpent = totalSpent;
        this.remaining = remaining;
        this.categoryCount = categoryCount;
        this.expenseCount = expenseCount;
        this.categoryBreakdown = categoryBreakdown;
        this.calamityCount = calamityCount;
        this.activeIncidents = activeIncidents;
    }

    public double getTotalBudget() { return totalBudget; }
    public double getTotalSpent() { return totalSpent; }
    public double getRemaining() { return remaining; }
    public long getCategoryCount() { return categoryCount; }
    public long getExpenseCount() { return expenseCount; }
    public List<CategoryBreakdownResponse> getCategoryBreakdown() { return categoryBreakdown; }
    public long getCalamityCount() { return calamityCount; }
    public long getActiveIncidents() { return activeIncidents; }
}
