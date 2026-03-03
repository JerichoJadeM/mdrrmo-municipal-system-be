package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByCategory (String category);

}
