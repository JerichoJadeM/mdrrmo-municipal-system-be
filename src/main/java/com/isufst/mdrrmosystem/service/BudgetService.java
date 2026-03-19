package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Budget;
import com.isufst.mdrrmosystem.entity.BudgetCategory;
import com.isufst.mdrrmosystem.entity.User;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.request.BudgetRequest;
import com.isufst.mdrrmosystem.response.*;
import com.isufst.mdrrmosystem.util.FindAuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final FindAuthenticatedUser findAuthenticatedUser;

    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         FindAuthenticatedUser findAuthenticatedUser) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.findAuthenticatedUser = findAuthenticatedUser;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudget() {
        return budgetRepository.findAll().stream()
                .map(budget -> new BudgetResponse(
                        budget.getId(),
                        budget.getYear(),
                        budget.getTotalAmount(),
                        budget.getDescription(),
                        budget.getCategories() == null
                                ? List.of()
                                : budget.getCategories().stream()
                                .map(category -> new CategoryResponse(
                                        category.getId(),
                                        category.getName(),
                                        category.getSection(),
                                        category.getAllocatedAmount()
                                ))
                                .toList(),
                        budget.getCreatedBy() != null
                                ? budget.getCreatedBy().getFirstName() + " " + budget.getCreatedBy().getLastName()
                                : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetDetailResponse getBudgetDetail(long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double totalObligations = expenseRepository.sumByBudgetId(budgetId);
        double remainingBalance = budget.getTotalAmount() - totalObligations;

        double allocatedToCategories = budget.getCategories() == null
                ? 0
                : budget.getCategories().stream()
                .mapToDouble(BudgetCategory::getAllocatedAmount)
                .sum();

        double unallocatedBudget = budget.getTotalAmount() - allocatedToCategories;

        double utilizationRate = budget.getTotalAmount() > 0
                ? (totalObligations / budget.getTotalAmount()) * 100
                : 0;

        List<CategoryResponse> categories = budget.getCategories() == null
                ? List.of()
                : budget.getCategories().stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getSection(),
                        category.getAllocatedAmount()
                ))
                .toList();

        return new BudgetDetailResponse(
                budget.getId(),
                budget.getYear(),
                budget.getTotalAmount(),
                totalObligations,
                remainingBalance,
                utilizationRate,
                allocatedToCategories,
                unallocatedBudget,
                budget.getDescription(),
                categories,
                budget.getCreatedBy() != null
                        ? budget.getCreatedBy().getFirstName() + " " + budget.getCreatedBy().getLastName()
                        : null
        );
    }

    @Transactional
    public Budget createBudget(BudgetRequest request) {
        User user = findAuthenticatedUser.getAuthenticatedUser();
        int currentYear = LocalDate.now().getYear();

        if (request.getYear() < currentYear) {
            throw new RuntimeException("Cannot create a budget for a past year.");
        }

        if (budgetRepository.existsByYear(request.getYear())) {
            throw new RuntimeException("A budget for year " + request.getYear() + " already exists.");
        }

        Budget budget = new Budget();
        budget.setYear(request.getYear());
        budget.setTotalAmount(request.getTotalAmount());
        budget.setDescription(request.getDescription());
        budget.setCreateAt(LocalDate.now());
        budget.setCreatedBy(user);

        return budgetRepository.save(budget);
    }

    @Transactional
    public BudgetResponse addToYearlyBudget(BudgetRequest request) {
        User user = findAuthenticatedUser.getAuthenticatedUser();
        int currentYear = LocalDate.now().getYear();

        if (request.getYear() < currentYear) {
            throw new RuntimeException("Cannot add budget to a past year.");
        }

        Budget budget = budgetRepository.findFirstByYear(request.getYear()).orElse(null);

        if (budget == null) {
            Budget newBudget = new Budget();
            newBudget.setYear(request.getYear());
            newBudget.setTotalAmount(request.getTotalAmount());
            newBudget.setDescription(request.getDescription());
            newBudget.setCreateAt(LocalDate.now());
            newBudget.setCreatedBy(user);

            Budget saved = budgetRepository.save(newBudget);

            return mapToBudgetResponse(saved);
        }

        budget.setTotalAmount(budget.getTotalAmount() + request.getTotalAmount());

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            budget.setDescription(budget.getDescription() + " | " + request.getDescription().trim());
        }

        Budget saved = budgetRepository.save(budget);
        return mapToBudgetResponse(saved);
    }

    private BudgetResponse mapToBudgetResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getYear(),
                budget.getTotalAmount(),
                budget.getDescription(),
                budget.getCategories() == null
                        ? List.of()
                        : budget.getCategories().stream()
                        .map(category -> new CategoryResponse(
                                category.getId(),
                                category.getName(),
                                category.getSection() != null ? category.getSection() : "UNASSIGNED",
                                category.getAllocatedAmount()
                        ))
                        .toList(),
                budget.getCreatedBy() != null
                        ? budget.getCreatedBy().getFirstName() + " " + budget.getCreatedBy().getLastName()
                        : null
        );
    }

    public double getTotalSpent(long budgetId) {
        return expenseRepository.sumByBudgetId(budgetId);
    }

    public double getRemainingBudget(long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        double totalSpent = getTotalSpent(budgetId);
        return budget.getTotalAmount() - totalSpent;
    }

    @Transactional(readOnly = true)
    public BudgetCurrentSummaryResponse getCurrentSummary() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        List<Budget> budgets = budgetRepository.findByYear(currentYear);

        if (budgets == null || budgets.isEmpty()) {
            return new BudgetCurrentSummaryResponse(
                    currentYear,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        Budget budget = budgets.get(0);

        double totalAllotment = budget.getTotalAmount();
        double totalObligations = expenseRepository.sumByBudgetId(budget.getId());
        double remainingBalance = totalAllotment - totalObligations;

        double allocatedToCategories = budget.getCategories() == null
                ? 0
                : budget.getCategories().stream()
                .mapToDouble(BudgetCategory::getAllocatedAmount)
                .sum();

        double unallocatedBudget = totalAllotment - allocatedToCategories;

        double utilizationRate = totalAllotment > 0
                ? (totalObligations / totalAllotment) * 100
                : 0;

        return new BudgetCurrentSummaryResponse(
                budget.getYear(),
                totalAllotment,
                totalObligations,
                remainingBalance,
                utilizationRate,
                allocatedToCategories,
                unallocatedBudget
        );
    }

    @Transactional(readOnly = true)
    public List<BudgetHistoryResponse> getBudgetHistory(int years) {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - Math.max(years - 1, 0);

        return budgetRepository.findAll().stream()
                .filter(budget -> budget.getYear() >= startYear && budget.getYear() <= currentYear)
                .sorted((a, b) -> Integer.compare(a.getYear(), b.getYear()))
                .map(budget -> {
                    double obligations = expenseRepository.sumByBudgetId(budget.getId());
                    double remaining = budget.getTotalAmount() - obligations;

                    return new BudgetHistoryResponse(
                            budget.getYear(),
                            budget.getTotalAmount(),
                            obligations,
                            remaining
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetForecastResponse getNextYearForecast() {
        int nextYear = LocalDate.now().getYear() + 1;

        List<BudgetHistoryResponse> history = getBudgetHistory(5);

        double averageAllotment = history.stream()
                .mapToDouble(BudgetHistoryResponse::allotment)
                .average()
                .orElse(0);

        double recommendedAdjustment = averageAllotment * 0.10;
        double totalForecast = averageAllotment + recommendedAdjustment;

        List<BudgetForecastCategoryResponse> categories = List.of(
                new BudgetForecastCategoryResponse(
                        "DISASTER PREPAREDNESS",
                        "Training Expenses",
                        totalForecast * 0.08,
                        "Preparedness capability-building forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER PREPAREDNESS",
                        "Traveling Expenses",
                        totalForecast * 0.04,
                        "Field coordination and preparedness travel forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER PREPAREDNESS",
                        "Rescue Equipment",
                        totalForecast * 0.07,
                        "Preparedness equipment upgrade and maintenance"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER PREVENTION AND MITIGATION",
                        "Other Supplies and Materials",
                        totalForecast * 0.08,
                        "Mitigation materials and preventive support forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER PREVENTION AND MITIGATION",
                        "Capital Outlay",
                        totalForecast * 0.10,
                        "Mitigation infrastructure and long-term investment forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER RESPONSE",
                        "Food and Water",
                        totalForecast * 0.14,
                        "Response relief supply forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER RESPONSE",
                        "Medical Supplies",
                        totalForecast * 0.08,
                        "Emergency medical supply forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER RESPONSE",
                        "Drugs and Medicines Expenses",
                        totalForecast * 0.07,
                        "Emergency medicine forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER RESPONSE",
                        "Evacuation Support",
                        totalForecast * 0.08,
                        "Evacuation center and displaced family support forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER RESPONSE",
                        "Maintenance and Other Operating Expenses",
                        totalForecast * 0.10,
                        "Response operational readiness forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER REHABILITATION AND RECOVERY",
                        "Subsidy to Other Funds",
                        totalForecast * 0.08,
                        "Recovery and rehabilitation support forecast"
                ),
                new BudgetForecastCategoryResponse(
                        "DISASTER REHABILITATION AND RECOVERY",
                        "Other Supplies and Materials",
                        totalForecast * 0.08,
                        "Recovery supply forecast"
                )
        );

        return new BudgetForecastResponse(
                nextYear,
                totalForecast,
                recommendedAdjustment,
                "Forecast based on recent budget history average with a 10% adjustment, aligned to MDRRM workbook sections and subcategories.",
                categories
        );
    }
}
