package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    List<AdminActionLog> findTop20ByOrderByCreatedAtDesc();

    @Query("""
        SELECT a
        FROM AdminActionLog a
        WHERE (:actionType IS NULL OR UPPER(a.actionType) = :actionType)
          AND (:performedBy IS NULL OR (
                a.actor IS NOT NULL AND
                UPPER(CONCAT(COALESCE(a.actor.firstName, ''), ' ', COALESCE(a.actor.lastName, ''))) LIKE CONCAT('%', :performedBy, '%')
              ))
          AND (:recordId IS NULL OR (a.targetUser IS NOT NULL AND a.targetUser.id = :recordId))
          AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
          AND (:toDate IS NULL OR a.createdAt < :toDate)
        ORDER BY a.createdAt DESC
    """)
    List<AdminActionLog> searchAuditTrail(@Param("actionType") String actionType,
                                          @Param("performedBy") String performedBy,
                                          @Param("recordId") Long recordId,
                                          @Param("fromDate") LocalDateTime fromDate,
                                          @Param("toDate") LocalDateTime toDate);
}