package com.isufst.mdrrmosystem.controller;

import com.isufst.mdrrmosystem.response.BarangaySyncResult;
import com.isufst.mdrrmosystem.service.BarangaySyncService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barangays/sync")
public class BarangaySyncController {

    private final BarangaySyncService barangaySyncService;

    public BarangaySyncController(BarangaySyncService barangaySyncService) {
        this.barangaySyncService = barangaySyncService;
    }

    @GetMapping("/batad")
    public BarangaySyncResult syncBatadBarangays(){
        return barangaySyncService.syncBatadBarangays();
    }

    @PostMapping
    public BarangaySyncResult syncByMunicipality(@RequestParam String municipality,
                                                 @RequestParam String province){
        return barangaySyncService.syncBarangaysByMunicipality(municipality,province);
    }
}
