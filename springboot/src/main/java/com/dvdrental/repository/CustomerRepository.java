package com.dvdrental.repository;

import com.dvdrental.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    @Query(value = """
        SELECT c.customer_id, c.first_name, c.last_name,
               COUNT(DISTINCT r.rental_id)  AS total_rentals,
               COALESCE(SUM(p.amount), 0)   AS total_spent,
               COALESCE(AVG(p.amount), 0)   AS avg_payment
        FROM customer c
        LEFT JOIN rental  r ON r.customer_id = c.customer_id
        LEFT JOIN payment p ON p.customer_id = c.customer_id
        WHERE c.customer_id = :customerId
        GROUP BY c.customer_id, c.first_name, c.last_name
        """, nativeQuery = true)
    List<Object[]> getCustomerStats(@Param("customerId") int customerId);

    @Query(value = """
        SELECT DISTINCT c.* FROM customer c
        JOIN rental r ON r.customer_id = c.customer_id
        JOIN inventory i ON i.inventory_id = r.inventory_id
        JOIN film f ON f.film_id = i.film_id
        WHERE r.return_date IS NULL
          AND r.rental_date + CAST((f.rental_duration || ' days') AS interval) < NOW()
        """, nativeQuery = true)
    List<Customer> findOverdueCustomers();
}
