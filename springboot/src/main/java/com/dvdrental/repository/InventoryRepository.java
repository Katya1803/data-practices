package com.dvdrental.repository;

import com.dvdrental.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Query(value = """
        SELECT i.* FROM inventory i
        WHERE i.film_id = :filmId AND i.store_id = :storeId
          AND i.inventory_id NOT IN (
              SELECT r.inventory_id FROM rental r WHERE r.return_date IS NULL
          )
        LIMIT 1
        """, nativeQuery = true)
    Optional<Inventory> findAvailableInventory(@Param("filmId") int filmId,
                                               @Param("storeId") int storeId);
}
