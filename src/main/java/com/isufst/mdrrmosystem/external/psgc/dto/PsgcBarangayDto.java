package com.isufst.mdrrmosystem.external.psgc.dto;

public record PsgcBarangayDto(
        String code,
        String name,
        String status,
        String region,
        String province,
        String city_municipality
) { }
