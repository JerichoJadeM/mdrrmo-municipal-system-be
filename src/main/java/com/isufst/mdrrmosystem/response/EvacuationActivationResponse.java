package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.time.LocalDateTime;

public record EvacuationActivationResponse(
        Long id,
        Integer currentEvacuees,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        Long incidentId,
        Long calamityId,
        Long centerId,
        String centerName,
        Integer centerCapacity,
        String centerBarangayName,
        String centerLocationDetails
) {
}
