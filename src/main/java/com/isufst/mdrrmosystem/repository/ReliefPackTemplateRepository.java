package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ReliefPackTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReliefPackTemplateRepository extends JpaRepository<ReliefPackTemplate, Long> {
    List<ReliefPackTemplate> findByActiveTrue();

    Optional<ReliefPackTemplate> findFirstByActiveTrueAndIntendedUseIgnoreCase(String intendedUse);

    Optional<ReliefPackTemplate> findFirstByActiveTrueAndNameContainingIgnoreCase(String keyword);
}
