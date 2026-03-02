package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}
