package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.ResponseActionRequest;
import com.isufst.mdrrmosystem.response.ResponseActionResponse;
import com.isufst.mdrrmosystem.service.ResponseActionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/response-actions/{incidentId}/actions")
public class ResponseActionController {

    private final ResponseActionService responseActionService;

    public ResponseActionController(ResponseActionService responseActionService) {
        this.responseActionService = responseActionService;
    }

    @PostMapping
    public ResponseActionResponse addAction(@PathVariable long incidentId, @RequestBody ResponseActionRequest responseActionRequest) {
        return responseActionService.addResponseAction(incidentId, responseActionRequest);
    }

    @GetMapping
    public List<ResponseActionResponse> getResponseActions(@PathVariable long incidentId) {
        return responseActionService.getByIncident(incidentId);
    }
}
