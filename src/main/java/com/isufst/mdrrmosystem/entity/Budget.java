package com.isufst.mdrrmosystem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    @Column(nullable = false, name = "year")
    private  int year;

    @Column(nullable = false, name = "total_amount")
    private double totalAmount;

    @Column(nullable = false, name = "description")
    private String description;

    @Column(nullable = false, name = "created_at")
    private LocalDate createAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL)
    private List<BudgetCategory> categories;

    public Budget() {}

    public Budget(int year, double totalAmount, String description, LocalDate createAt, User createdBy, List<BudgetCategory> categories) {
        this.year = year;
        this.totalAmount = totalAmount;
        this.description = description;
        this.createAt = createAt;
        this.createdBy = createdBy;
        this.categories = categories;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDate createAt) {
        this.createAt = createAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<BudgetCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<BudgetCategory> categories) {
        this.categories = categories;
    }
}
