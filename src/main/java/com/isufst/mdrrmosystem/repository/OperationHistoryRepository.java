package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.OperationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {

    List<OperationHistory> findByOperationTypeAndOperationIdOrderByPerformedAtDesc(String operationType, Long operationId);

    @Query("""
        SELECT oh
        FROM OperationHistory oh
        WHERE (:operationType IS NULL OR UPPER(oh.operationType) = :operationType)
          AND (:actionType IS NULL OR UPPER(oh.actionType) = :actionType)
          AND (:performedBy IS NULL OR UPPER(oh.performedBy) LIKE CONCAT('%', :performedBy, '%'))
          AND (:operationId IS NULL OR oh.operationId = :operationId)
          AND (:fromDate IS NULL OR oh.performedAt >= :fromDate)
          AND (:toDate IS NULL OR oh.performedAt < :toDate)
        ORDER BY oh.performedAt DESC
    """)
    List<OperationHistory> searchAuditTrail(@Param("operationType") String operationType,
                                            @Param("actionType") String actionType,
                                            @Param("performedBy") String performedBy,
                                            @Param("operationId") Long operationId,
                                            @Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDate") LocalDateTime toDate);

    @Query("""
        SELECT COUNT(oh)
        FROM OperationHistory oh
        WHERE (:operationType IS NULL OR UPPER(oh.operationType) = :operationType)
          AND (:actionType IS NULL OR UPPER(oh.actionType) = :actionType)
          AND (:performedBy IS NULL OR UPPER(oh.performedBy) LIKE CONCAT('%', :performedBy, '%'))
          AND (:operationId IS NULL OR oh.operationId = :operationId)
          AND (:fromDate IS NULL OR oh.performedAt >= :fromDate)
          AND (:toDate IS NULL OR oh.performedAt < :toDate)
    """)
    long countAuditTrail(@Param("operationType") String operationType,
                         @Param("actionType") String actionType,
                         @Param("performedBy") String performedBy,
                         @Param("operationId") Long operationId,
                         @Param("fromDate") LocalDateTime fromDate,
                         @Param("toDate") LocalDateTime toDate);
}

