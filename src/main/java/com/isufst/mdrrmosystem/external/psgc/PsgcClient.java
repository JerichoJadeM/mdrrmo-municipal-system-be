package com.isufst.mdrrmosystem.external.psgc;

import com.isufst.mdrrmosystem.external.psgc.dto.PsgcBarangayDto;

import java.util.List;

public interface PsgcClient {

    List<PsgcBarangayDto> getBarangaysByCityMunicipality(String cityMunicipalityName);
}
