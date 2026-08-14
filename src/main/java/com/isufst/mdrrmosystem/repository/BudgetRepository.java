package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByYear(int year);

    Optional<Budget> findFirstByYear(int year);
    boolean existsByYear(int year);
    List<Budget> findAllByOrderByYearAsc();

    @Modifying
    @Query("update Budget b set b.createdBy = null where b.createdBy.id = :userId")
    void detachCreatedBy(@Param("userId") Long userId);
}
