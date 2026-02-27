package com.isufst.mdrrmosystem.request;

public record EvacuationCenterRequest(
        String name,
        String barangay,
        int capacity,
        String locationDetails
) {
}
