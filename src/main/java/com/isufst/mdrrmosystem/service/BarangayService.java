package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Barangay;
import com.isufst.mdrrmosystem.repository.BarangayRepository;
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

    public List<Barangay> findAllBarangay() {
        return barangayRepository.findAll();
    }
}
