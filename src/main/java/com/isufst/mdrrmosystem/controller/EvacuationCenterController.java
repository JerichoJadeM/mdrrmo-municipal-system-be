package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.EvacuationCenterRequest;
import com.isufst.mdrrmosystem.response.EvacuationCenterResourceResponse;
import com.isufst.mdrrmosystem.response.EvacuationCenterResponse;
import com.isufst.mdrrmosystem.service.EvacuationCenterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evacuation-centers")
public class EvacuationCenterController {

    private final EvacuationCenterService evacuationCenterService;

    public EvacuationCenterController(EvacuationCenterService evacuationCenterService) {
        this.evacuationCenterService = evacuationCenterService;
    }

    @PostMapping
    public EvacuationCenterResponse addCenter(@RequestBody EvacuationCenterRequest request) {
        return evacuationCenterService.addEvacuationCenter(request);
    }

    @GetMapping
    public List<EvacuationCenterResponse> getEvacuationCenters() {
        return evacuationCenterService.getCenterList();
    }

    @PutMapping("/{id}")
    public EvacuationCenterResponse updateCenter(@PathVariable long id,
                                                 @RequestBody EvacuationCenterRequest request) {
        return evacuationCenterService.updateEvacuationCenter(id, request);
    }

    @GetMapping("/resources-view")
    public List<EvacuationCenterResourceResponse> getResourcesView(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String usage) {
        return evacuationCenterService.getResourcesView(keyword, status, usage);
    }
}
