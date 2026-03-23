package com.isufst.mdrrmosystem.response;

public record CountByLabelResponse(
        String label,
        long count
) {
}
