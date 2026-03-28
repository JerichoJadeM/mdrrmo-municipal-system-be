package com.isufst.mdrrmosystem.response;

public record WarningItem(
        String level,
        String code,
        String message,
        String recommendation,
        boolean requiresAcknowledgement,
        boolean requiresManagerApproval
) {}
