package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Barangay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BarangayRepository extends JpaRepository<Barangay, Long> {

    @Query("""
        SELECT b
        FROM Barangay b
        WHERE LOWER(b.municipalityName) = 'batad'
          AND LOWER(b.provinceName) = 'iloilo'
          AND b.active = true
        ORDER BY b.name ASC
    """)
    List<Barangay> findActiveBatadBarangays();

    Optional<Barangay> findByPsgcCode(String psgcCode);

    List<Barangay> findByMunicipalityNameIgnoreCaseAndProvinceNameIgnoreCase(String municipalityName, String provinceName);
}
