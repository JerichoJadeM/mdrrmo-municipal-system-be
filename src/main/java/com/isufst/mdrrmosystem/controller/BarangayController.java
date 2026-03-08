package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.response.BarangayResponse;
import com.isufst.mdrrmosystem.service.BarangayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/barangays")
public class BarangayController {

    private final BarangayService barangayService;

    public BarangayController(BarangayService barangayService) {
        this.barangayService = barangayService;
    }

    @PostMapping
    public Barangay addBarangay(@RequestBody Barangay barangay) {
        return barangayService.addBarangay(barangay);
    }

    @GetMapping("/all")
    public List<Barangay> getAllBarangay() {
        return barangayService.findAllBarangay();
    }

    @GetMapping
    public List<BarangayResponse> getBatadBarangays(){
        return barangayService.getBatadBarangays();
    }
}
