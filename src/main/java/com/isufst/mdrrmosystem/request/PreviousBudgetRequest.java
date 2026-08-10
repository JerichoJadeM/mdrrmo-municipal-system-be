package com.isufst.mdrrmosystem.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PreviousBudgetRequest(
        Integer year,
        Double allotment,
        @JsonAlias({"obligation", "obligations"}) Double obligations,
        Double remaining,
        Double utilizationRate,
        String description
) {
}
