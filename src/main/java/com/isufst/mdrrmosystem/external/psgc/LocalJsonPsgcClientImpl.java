package com.isufst.mdrrmosystem.external.psgc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isufst.mdrrmosystem.external.psgc.dto.PsgcBarangayDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Component
public class LocalJsonPsgcClientImpl {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<PsgcBarangayDto> getBatadBarangays() {

        String path = "data/barangays-batad-iloilo/json";
        ClassPathResource resource = new ClassPathResource(path);

        if(!resource.exists()) {
            System.err.println("CRITICAL: Local JSON file not found at: " + path);
            return Collections.emptyList();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<PsgcBarangayDto>>() {});

        }catch (IOException e) {
            System.err.println("ERROR: Failed to parse local JSON: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
