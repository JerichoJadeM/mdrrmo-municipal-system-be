package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    List<RiskAssessment> findTop20ByOrderByAssessedAtDesc();
}
