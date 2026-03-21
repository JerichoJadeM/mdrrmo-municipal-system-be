package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.ReliefPackTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReliefPackTemplateRepository extends JpaRepository<ReliefPackTemplate, Long> {
    List<ReliefPackTemplate> findByActiveTrue();
}
