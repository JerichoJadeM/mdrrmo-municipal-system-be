package com.isufst.mdrrmosystem.request;

import java.util.List;

public record UpdateUserRoleRequest(
        List<String> authorities
) {}
