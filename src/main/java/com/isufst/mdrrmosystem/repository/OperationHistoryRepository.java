package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.OperationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {

    List<OperationHistory> findByOperationTypeAndOperationIdOrderByPerformedAtDesc(String operationType, Long operationId);
}
