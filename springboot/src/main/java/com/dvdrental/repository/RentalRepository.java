package com.dvdrental.repository;

import com.dvdrental.model.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Integer> {

    @EntityGraph(attributePaths = {"inventory", "inventory.film", "customer", "staff"})
    Page<Rental> findByReturnDateIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"inventory", "inventory.film", "customer", "staff"})
    Optional<Rental> findById(Integer id);

    @Query(value = """
        SELECT r.* FROM rental r
        JOIN inventory i ON i.inventory_id = r.inventory_id
        JOIN film f ON f.film_id = i.film_id
        WHERE r.return_date IS NULL
          AND r.rental_date + CAST((f.rental_duration || ' days') AS interval) < NOW()
        """, nativeQuery = true)
    List<Rental> findOverdueRentalsRaw();

    @EntityGraph(attributePaths = {"inventory", "inventory.film", "customer", "staff"})
    @Query("SELECT r FROM Rental r WHERE r.customer.customerId = :customerId ORDER BY r.rentalDate DESC")
    Page<Rental> findByCustomerId(@Param("customerId") int customerId, Pageable pageable);

    @Query(value = """
        SELECT COUNT(*) > 0 FROM rental r
        WHERE r.inventory_id = :inventoryId AND r.return_date IS NULL
        """, nativeQuery = true)
    boolean isInventoryRented(@Param("inventoryId") int inventoryId);
}
