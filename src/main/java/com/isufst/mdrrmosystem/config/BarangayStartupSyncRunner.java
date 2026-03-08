package com.isufst.mdrrmosystem.config;

import com.isufst.mdrrmosystem.service.BarangaySyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BarangayStartupSyncRunner implements CommandLineRunner {

    private final BarangaySyncService barangaySyncService;

    public BarangayStartupSyncRunner(BarangaySyncService barangaySyncService) {
        this.barangaySyncService = barangaySyncService;
    }

    @Override
    public void run(String... args) {
        barangaySyncService.syncBatadBarangays();
    }
}
