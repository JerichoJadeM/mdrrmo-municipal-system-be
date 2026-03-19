package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByYear(int year);

    Optional<Budget> findFirstByYear(int year);
    boolean existsByYear(int year);
}
