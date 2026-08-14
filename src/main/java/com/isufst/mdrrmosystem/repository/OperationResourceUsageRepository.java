package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.OperationResourceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperationResourceUsageRepository extends JpaRepository<OperationResourceUsage, Long> {

    List<OperationResourceUsage> findByOperationTypeAndOperationIdOrderByRecordedAtDesc(String operationType, Long operationId);

    @Query("""
        SELECT COALESCE(SUM(u.lineTotal), 0)
        FROM OperationResourceUsage u
        WHERE u.operationType = :operationType AND u.operationId = :operationId
    """)
    Double sumLineTotalByOperationTypeAndOperationId(@Param("operationType") String operationType,
                                                     @Param("operationId") Long operationId);

    @Modifying
    @Query("update OperationResourceUsage u set u.recordedBy = null where u.recordedBy.id = :userId")
    void detachRecordedBy(@Param("userId") Long userId);
}
