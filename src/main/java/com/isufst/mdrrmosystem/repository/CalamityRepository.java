package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Calamity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CalamityRepository extends JpaRepository<Calamity, Long> {

    List<Calamity> findByDateBetween(LocalDate start, LocalDate end);

    long countByDateBetween(LocalDate start, LocalDate end);
}
