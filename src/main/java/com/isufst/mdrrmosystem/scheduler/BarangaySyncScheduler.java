package com.isufst.mdrrmosystem.scheduler;

import com.isufst.mdrrmosystem.service.BarangaySyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BarangaySyncScheduler {

    private final BarangaySyncService barangaySyncService;

    public BarangaySyncScheduler(BarangaySyncService barangaySyncService) {
        this.barangaySyncService = barangaySyncService;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Manila")
    public void syncBatadBarangaysNightly(){
        try {
            barangaySyncService.syncBatadBarangays();
        } catch (Exception ex) {
            System.err.println("Scheduled barangay sync failed: " + ex.getMessage());
        }
    }
}
