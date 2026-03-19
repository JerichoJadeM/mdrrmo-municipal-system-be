package com.isufst.mdrrmosystem.response;

public record CategoryResponse(
        long id,
        String name,
        String section,
        double allocatedAmount
) { }
