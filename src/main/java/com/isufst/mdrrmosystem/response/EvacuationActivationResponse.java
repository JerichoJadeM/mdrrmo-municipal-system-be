package com.isufst.mdrrmosystem.response;

import com.isufst.mdrrmosystem.entity.Barangay;

import java.time.LocalDateTime;

public record EvacuationActivationResponse(
        Long id,
        String centerName,
        Barangay barangay,
        int capacity,
        int currentEvacuees,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
}
