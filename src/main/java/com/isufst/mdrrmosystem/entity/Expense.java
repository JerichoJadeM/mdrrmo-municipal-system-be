package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false, name = "expense_date")
    private LocalDate expenseDate;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private BudgetCategory category;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
