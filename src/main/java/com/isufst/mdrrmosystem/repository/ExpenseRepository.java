package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.response.CategoryBreakdownResponse;
import com.isufst.mdrrmosystem.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
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

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.incident.id = :incidentId
    """)
    Double sumByIncidentId(@Param("incidentId") Long incidentId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.calamity.id = :calamityId
    """)
    Double sumByCalamityId(@Param("calamityId") Long calamityId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.category.id = :categoryId
    """)
    Double sumByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT e
        FROM Expense e
        WHERE (:performedBy IS NULL OR (
                e.createdBy IS NOT NULL AND
                UPPER(CONCAT(COALESCE(e.createdBy.firstName, ''), ' ', COALESCE(e.createdBy.lastName, ''))) LIKE CONCAT('%', :performedBy, '%')
              ))
          AND (:recordId IS NULL OR e.id = :recordId)
          AND (:fromDate IS NULL OR e.expenseDate >= :fromDate)
          AND (:toDate IS NULL OR e.expenseDate < :toDate)
        ORDER BY e.expenseDate DESC, e.id DESC
    """)
    List<Expense> searchAuditTrail(@Param("performedBy") String performedBy,
                                   @Param("recordId") Long recordId,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate);
}