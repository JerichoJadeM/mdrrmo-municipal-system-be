package com.isufst.mdrrmosystem.response;

public record BarangaySyncResult(
        int fetchedCount,
        int insertedCount,
        int updatedCount,
        int deactivatedCount,
        String municipalityName,
        String provinceName
) {
}
