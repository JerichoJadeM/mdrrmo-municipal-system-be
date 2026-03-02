package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

public record EvacuationCenterResponse(
        Long id,
        String name,
        Barangay barangay,
        int capacity,
        String locationDetails
) {
}
