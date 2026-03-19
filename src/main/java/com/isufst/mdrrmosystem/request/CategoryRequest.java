package com.isufst.mdrrmosystem.request;

public record CategoryRequest(
        String name,
        String section,
        double allocatedAmount
) { }
