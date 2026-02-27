package com.isufst.mdrrmosystem.request;

public record EvacuationActivationRequest(
        Long centerId,
        int initialEvacuees
) {
}
