package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.OperationHistory;
import com.isufst.mdrrmosystem.repository.OperationHistoryRepository;
import com.isufst.mdrrmosystem.response.OperationsHistoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationHistoryService {

    private final OperationHistoryRepository operationHistoryRepository;

    public OperationHistoryService(OperationHistoryRepository operationHistoryRepository) {
        this.operationHistoryRepository = operationHistoryRepository;
    }

    @Transactional
    public void log(String operationType,
                    Long operationId,
                    String actionType,
                    String fromStatus,
                    String toStatus,
                    String description,
                    String metadataJson,
                    String performedBy) {

        OperationHistory history = new OperationHistory();
        history.setOperationType(operationType);
        history.setOperationId(operationId);
        history.setActionType(actionType);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setDescription(description);
        history.setMetadataJson(metadataJson);
        history.setPerformedBy(performedBy);
        history.setPerformedAt(LocalDateTime.now());

        operationHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<OperationsHistoryResponse> getHistory(String operationType, Long operationId) {
        return operationHistoryRepository
                .findByOperationTypeAndOperationIdOrderByPerformedAtDesc(operationType.toUpperCase(), operationId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OperationsHistoryResponse mapToResponse(OperationHistory history) {
        return new OperationsHistoryResponse(
                history.getId(),
                history.getOperationType(),
                history.getOperationId(),
                history.getActionType(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getDescription(),
                history.getMetadataJson(),
                history.getPerformedBy(),
                history.getPerformedAt()
        );
    }
}
