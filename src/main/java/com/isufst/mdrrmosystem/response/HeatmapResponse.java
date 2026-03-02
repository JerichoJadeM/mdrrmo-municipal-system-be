package com.isufst.mdrrmosystem.response;

public record HeatmapResponse(
        Long id,
        String barangayName,
        long incidentCount
) {
}
