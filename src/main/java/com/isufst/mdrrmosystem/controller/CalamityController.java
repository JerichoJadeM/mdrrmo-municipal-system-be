package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.request.CalamityRequest;
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
}
