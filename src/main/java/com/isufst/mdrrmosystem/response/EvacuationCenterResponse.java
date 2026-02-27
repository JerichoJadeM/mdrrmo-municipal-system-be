package com.isufst.mdrrmosystem.response;

public record EvacuationCenterResponse(
        Long id,
        String name,
        String barangay,
        int capacity,
        String locationDetails
) {
}
