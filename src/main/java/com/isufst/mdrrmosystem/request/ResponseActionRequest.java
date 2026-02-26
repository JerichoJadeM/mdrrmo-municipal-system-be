package com.isufst.mdrrmosystem.request;

public record ResponseActionRequest(
        String actionType,
        String description,
        Long responderId
) {
}
