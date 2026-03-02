package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    WeatherData findTopByOrderByRecordedAtDesc();

}
