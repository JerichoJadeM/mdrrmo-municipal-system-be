package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    @Query("""
        SELECT t
        FROM InventoryTransaction t
        WHERE (:fromDate IS NULL OR t.timeStamp >= :fromDate)
          AND (:toDate IS NULL OR t.timeStamp < :toDate)
        ORDER BY t.timeStamp DESC
    """)
    List<InventoryTransaction> findAllWithin(@Param("fromDate") LocalDateTime fromDate,
                                             @Param("toDate") LocalDateTime toDate);
}
