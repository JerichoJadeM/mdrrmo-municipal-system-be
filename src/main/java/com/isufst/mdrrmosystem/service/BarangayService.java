package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
import com.isufst.mdrrmosystem.response.BarangayResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BarangayService {

    private final BarangayRepository barangayRepository;

    public BarangayService(BarangayRepository barangayRepository) {
        this.barangayRepository = barangayRepository;
    }

    @Transactional
    public Barangay addBarangay(Barangay barangay) {
        return barangayRepository.save(barangay);
    }

    @Transactional(readOnly = true)
    public List<Barangay> findAllBarangay() {
        return barangayRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<BarangayResponse> getBatadBarangays() {
        return barangayRepository.findActiveBatadBarangays()
                .stream()
                .map(barangay -> new BarangayResponse(barangay.getId(), barangay.getName()))
                .toList();
    }
}