package com.isufst.mdrrmosystem.external.psgc.dto;

import java.util.List;

public record PsgcResponseWrapper(
        List<PsgcBarangayDto> data
) {
}
