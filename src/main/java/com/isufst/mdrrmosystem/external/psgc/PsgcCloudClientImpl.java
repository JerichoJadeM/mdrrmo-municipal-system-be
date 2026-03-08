package com.isufst.mdrrmosystem.external.psgc;

import com.isufst.mdrrmosystem.external.psgc.dto.PsgcBarangayDto;
import com.isufst.mdrrmosystem.external.psgc.dto.PsgcResponseWrapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class PsgcCloudClientImpl implements PsgcClient{

    private final RestTemplate restTemplate;

    public PsgcCloudClientImpl(){
        this.restTemplate = new RestTemplate();
    }

    @Override
    public List<PsgcBarangayDto> getBarangaysByCityMunicipality(String cityMunicipalityName) {
        // Note: You may need the specific city/municipality CODE,
        // but if you're using names, ensure the API supports it.
        String encodedName = UriUtils.encodePathSegment(cityMunicipalityName, StandardCharsets.UTF_8);
        String url = "https://psgc.cloud/api/v2/cities-municipalities/" + encodedName + "/barangays";

        PsgcResponseWrapper response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PsgcResponseWrapper>() {}
        ).getBody();

        return (response != null) ? response.data() : Collections.emptyList();
    }
}
