package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.external.psgc.PsgcClient;
import com.isufst.mdrrmosystem.external.psgc.dto.PsgcBarangayDto;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.response.BarangaySyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BarangaySyncService{

    private final BarangayRepository barangayRepository;
    private final PsgcClient psgcClient;

    public BarangaySyncService(BarangayRepository barangayRepository, PsgcClient psgcClient) {
        this.barangayRepository = barangayRepository;
        this.psgcClient = psgcClient;
    }

    
    @Transactional
    public BarangaySyncResult syncBatadBarangays() {
        return syncBarangaysByMunicipality("Batad", "Iloilo");
    }

    
    @Transactional
    public BarangaySyncResult syncBarangaysByMunicipality(String municipalityName, String provinceName) {
        List<PsgcBarangayDto> externalBarangays = psgcClient.getBarangaysByCityMunicipality(municipalityName);

        if (externalBarangays == null) {
            externalBarangays = Collections.emptyList();
        }

        List<PsgcBarangayDto> filtered = externalBarangays.stream()
                .filter(dto -> dto.province() != null && dto.province().equalsIgnoreCase(provinceName))
                .filter(dto -> dto.city_municipality() != null && dto.city_municipality().equalsIgnoreCase(municipalityName))
                .toList();

        Map<String, Barangay> existingByCode = barangayRepository
                .findByMunicipalityNameIgnoreCaseAndProvinceNameIgnoreCase(municipalityName, provinceName)
                .stream()
                .filter(b -> b.getPsgcCode() != null)
                .collect(Collectors.toMap(
                        Barangay::getPsgcCode,
                        b -> b,
                        (existing, replacement) -> existing // Merge function: keep the first one
                ));

        Set<String> incomingCodes = new HashSet<>();

        int inserted = 0;
        int updated = 0;
        int deactivated = 0;

        for (PsgcBarangayDto dto : filtered) {
            incomingCodes.add(dto.code());

            Barangay barangay = existingByCode.get(dto.code());
            if (barangay == null) {
                barangay = new Barangay();
                barangay.setPsgcCode(dto.code());
                inserted++;
            } else {
                updated++;
            }

            barangay.setName(dto.name());
            barangay.setMunicipalityName(municipalityName);
            barangay.setProvinceName(provinceName);
            barangay.setActive(true);

            if (barangay.getPopulation() < 0) {
                barangay.setPopulation(0);
            }

            if (barangay.getRiskLevel() == null || barangay.getRiskLevel().isBlank()) {
                barangay.setRiskLevel("LOW");
            }

            barangayRepository.save(barangay);
        }

        List<Barangay> existingMunicipalityBarangays =
                barangayRepository.findByMunicipalityNameIgnoreCaseAndProvinceNameIgnoreCase(municipalityName, provinceName);

        for (Barangay barangay : existingMunicipalityBarangays) {
            if (barangay.getPsgcCode() != null && !incomingCodes.contains(barangay.getPsgcCode())) {
                if (Boolean.TRUE.equals(barangay.getActive())) {
                    barangay.setActive(false);
                    barangayRepository.save(barangay);
                    deactivated++;
                }
            }
        }

        return new BarangaySyncResult(
                filtered.size(),
                inserted,
                updated,
                deactivated,
                municipalityName,
                provinceName
        );
    }
}