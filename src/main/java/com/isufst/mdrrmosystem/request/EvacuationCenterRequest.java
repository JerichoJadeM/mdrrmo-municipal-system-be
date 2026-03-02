package com.isufst.mdrrmosystem.request;

import com.isufst.mdrrmosystem.entity.Barangay;

public record EvacuationCenterRequest(
        String name,
        Barangay barangay,
        int capacity,
        String locationDetails
) {
}
