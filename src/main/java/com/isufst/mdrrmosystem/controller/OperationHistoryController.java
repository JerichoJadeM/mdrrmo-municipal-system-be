package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.repository.OperationHistoryRepository;
import com.isufst.mdrrmosystem.response.OperationsHistoryResponse;
import com.isufst.mdrrmosystem.service.OperationHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operations/history")
public class OperationHistoryController {

    private final OperationHistoryService operationHistoryService;

    public OperationHistoryController(OperationHistoryService operationHistoryService) {
        this.operationHistoryService = operationHistoryService;
    }

    @GetMapping
    public List<OperationsHistoryResponse> getOperationHistory(@RequestParam String type,
                                                               @RequestParam Long id) {
        return operationHistoryService.getHistory(type, id);
    }
}
