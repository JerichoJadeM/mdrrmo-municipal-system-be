package com.isufst.mdrrmosystem.response;

public record CategoryResponse(
        long id,
        String section,
        String name,
        double allocatedAmount
) { }
