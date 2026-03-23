package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Calamity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalamityRepository extends JpaRepository<Calamity, Long> {

    List<Calamity> findByDateBetween(LocalDate start, LocalDate end);

    long countByDateBetween(LocalDate start, LocalDate end);

    @Query("""
        SELECT COUNT(c)
        FROM Calamity c
        WHERE (:fromDate IS NULL OR c.date >= :fromDate)
          AND (:toDate IS NULL OR c.date <= :toDate)
    """)
    long countAllWithin(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT COUNT(c)
        FROM Calamity c
        WHERE c.status = :status
          AND (:fromDate IS NULL OR c.date >= :fromDate)
          AND (:toDate IS NULL OR c.date <= :toDate)
    """)
    long countByStatusWithin(@Param("status") String status,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT c
        FROM Calamity c
        WHERE (:fromDate IS NULL OR c.date >= :fromDate)
          AND (:toDate IS NULL OR c.date <= :toDate)
        ORDER BY c.date DESC
    """)
    List<Calamity> findAllWithinRange(@Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate);
}
