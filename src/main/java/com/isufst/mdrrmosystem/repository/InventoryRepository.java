package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByCategory (String category);

    @Query("""
        SELECT i
        FROM Inventory i
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR :category = '' OR UPPER(i.category) = UPPER(:category))
        ORDER BY i.name ASC
    """)
    List<Inventory> findForResourcesView(@Param("keyword") String keyword,
                                         @Param("category") String category);

    @Query("""
        SELECT COUNT(i) 
        FROM Inventory i 
        WHERE i.availableQuantity <= i.reorderLevel
    """)
    long countLowStockItems();

    @Query("""
        SELECT COUNT(i)
        FROM Inventory i
        WHERE i.availableQuantity = 0 AND i.criticalItem = true
    """)
    long countCriticalOutOfStock();

    @Query("""
        SELECT COUNT(i)
        FROM Inventory i
        WHERE i.availableQuantity <= i.reorderLevel AND i.criticalItem = true
    """)
    long countCriticalLowStock();
}
