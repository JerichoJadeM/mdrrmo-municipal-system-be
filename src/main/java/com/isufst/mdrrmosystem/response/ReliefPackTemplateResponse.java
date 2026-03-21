package com.isufst.mdrrmosystem.response;

import java.util.List;

public record ReliefPackTemplateResponse(
        Long id,
        String name,
        String packType,
        String intendedUse,
        boolean active,
        List<ReliefPackTemplateItemResponse> items
) {
}
