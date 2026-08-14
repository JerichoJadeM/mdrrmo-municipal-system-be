package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.CalamityRequest;
import com.isufst.mdrrmosystem.request.CalamityTransitionRequest;
import com.isufst.mdrrmosystem.response.CalamityResponse;
import com.isufst.mdrrmosystem.service.CalamityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calamities")
public class CalamityController {

    private final CalamityService calamityService;

    public CalamityController(CalamityService calamityService) {
        this.calamityService = calamityService;
    }

    @PostMapping
    public CalamityResponse createCalamityRecord(@Valid @RequestBody CalamityRequest calamityRequest){
        return calamityService.addCalamityRecord(calamityRequest);
    }

    @GetMapping
    public List<CalamityResponse> getCalamities(){
        return calamityService.getAllCalamityRecords();
    }

    @GetMapping("/archived")
    public List<CalamityResponse> getArchivedCalamities() {
        return calamityService.getArchivedCalamities();
    }

    @PutMapping("/{id}")
    public CalamityResponse updateCalamityRecord(@PathVariable Long id,
                                                 @Valid @RequestBody CalamityRequest calamityRequest){
        return calamityService.updateCalamityRecord(id, calamityRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteCalamityRecord(@PathVariable long id){
        calamityService.deleteCalamityRecord(id);
    }

    // test
    @GetMapping("/{id}")
    public CalamityResponse getCalamityById(@PathVariable long id) {
        return calamityService.getCalamityById(id);
    }

    @PutMapping("/{id}/monitor")
    public CalamityResponse markMonitoring(@PathVariable long id,
                                           @RequestBody(required = false) CalamityTransitionRequest request) {
        return calamityService.markCalamityMonitoring(id, request);
    }

    @PutMapping("/{id}/resolve")
    public CalamityResponse markResolved(@PathVariable long id,
                                         @RequestBody(required = false) CalamityTransitionRequest request) {
        return calamityService.markCalamityResolved(id, request);
    }

    @PutMapping("/{id}/end")
    public CalamityResponse markEnded(@PathVariable long id,
                                      @RequestBody(required = false) CalamityTransitionRequest request) {
        return calamityService.markCalamityEnded(id, request);
    }

    @PutMapping("/{id}/archive")
    public CalamityResponse archiveCalamityRecord(@PathVariable long id) {
        return calamityService.archiveCalamityRecord(id);
    }

    @PutMapping("/{id}/restore")
    public CalamityResponse restoreCalamityRecord(@PathVariable long id) {
        return calamityService.restoreCalamityRecord(id);
    }

    @PutMapping("/{id}/archive/clear")
    public CalamityResponse clearArchivedCalamity(@PathVariable long id) {
        return calamityService.clearArchivedCalamity(id);
    }

    @PutMapping("/archived/restore-all")
    public List<CalamityResponse> restoreAllArchivedCalamities() {
        return calamityService.restoreAllArchivedCalamities();
    }

    @PutMapping("/archived/clear-all")
    public List<CalamityResponse> clearAllArchivedCalamities() {
        return calamityService.clearAllArchivedCalamities();
    }
}
