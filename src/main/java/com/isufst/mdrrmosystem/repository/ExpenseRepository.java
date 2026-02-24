package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Expense;
import com.isufst.mdrrmosystem.response.CategoryBreakdownResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT COALESCE (SUM(e.amount), 0)
        FROM Expense e
        WHERE e.category.budget.id = :budgetId
    """)
    Double sumByBudgetId(@Param("budgetId") Long budgetId);

    Long countByCategory_Budget_Id(Long budgetId);

    @Query("""
        SELECT new com.isufst.mdrrmosystem.response.CategoryBreakdownResponse(
            c.name,
            COALESCE(SUM(e.amount), 0)
        )
        FROM Expense e
        JOIN e.category c
        GROUP BY c.name
    """)
    List<CategoryBreakdownResponse> getCategoryBreakdown();

}
