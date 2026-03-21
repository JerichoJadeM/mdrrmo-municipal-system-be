package com.isufst.mdrrmosystem.request;

import java.util.List;

public record ReliefPackTemplateRequest(
        String name,
        String packType,
        String intendedUse,
        Boolean active,
        List<ReliefPackTemplateItemRequest> items
) {
}
