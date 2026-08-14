package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.PreviousBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreviousBudgetRepository extends JpaRepository<PreviousBudget, Long> {

    Optional<PreviousBudget> findByYear(Integer year);

    boolean existsByYear(Integer year);

    List<PreviousBudget> findAllByOrderByYearAsc();
}
