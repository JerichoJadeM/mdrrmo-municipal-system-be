package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.InventoryTransaction;
import com.isufst.mdrrmosystem.response.TopConsumedResourceResponse;
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

    @Query("""
        SELECT t
        FROM InventoryTransaction t
        WHERE (:actionType IS NULL OR UPPER(t.actionType) = :actionType)
          AND (:performedBy IS NULL OR (
                t.performedBy IS NOT NULL AND
                UPPER(CONCAT(COALESCE(t.performedBy.firstName, ''), ' ', COALESCE(t.performedBy.lastName, ''))) LIKE CONCAT('%', :performedBy, '%')
              ))
          AND (:recordId IS NULL OR t.id = :recordId)
          AND (:fromDate IS NULL OR t.timeStamp >= :fromDate)
          AND (:toDate IS NULL OR t.timeStamp < :toDate)
        ORDER BY t.timeStamp DESC
    """)
    List<InventoryTransaction> searchAuditTrail(@Param("actionType") String actionType,
                                                @Param("performedBy") String performedBy,
                                                @Param("recordId") Long recordId,
                                                @Param("fromDate") LocalDateTime fromDate,
                                                @Param("toDate") LocalDateTime toDate);


    @Query("""
        SELECT new com.isufst.mdrrmosystem.response.TopConsumedResourceResponse(
            i.name,
            COALESCE(SUM(t.quantity), 0)
        )
        FROM InventoryTransaction t
        JOIN t.inventory i
        WHERE UPPER(t.actionType) = 'CONSUMED'
          AND (:fromDate IS NULL OR t.timeStamp >= :fromDate)
          AND (:toDate IS NULL OR t.timeStamp < :toDate)
        GROUP BY i.id, i.name
        ORDER BY COALESCE(SUM(t.quantity), 0) DESC, i.name ASC
    """)
    List<TopConsumedResourceResponse> findTopConsumedResources(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            org.springframework.data.domain.Pageable pageable
    );
}
