package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByCategory (String category);
}
