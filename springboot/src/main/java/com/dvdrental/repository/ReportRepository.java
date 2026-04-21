package com.dvdrental.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends Repository<com.dvdrental.model.Film, Integer> {

    @Query(value = """
        SELECT f.film_id, f.title, COUNT(r.rental_id) AS rental_count
        FROM film f
        JOIN inventory i ON i.film_id = f.film_id
        JOIN rental r ON r.inventory_id = i.inventory_id
        GROUP BY f.film_id, f.title
        ORDER BY rental_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopFilms(@Param("limit") int limit);

    @Query(value = """
        SELECT
            DATE_PART('month', payment_date) AS month,
            SUM(amount)                       AS revenue,
            COUNT(*)                          AS transaction_count
        FROM payment
        WHERE DATE_PART('year', payment_date) = :year
        GROUP BY month
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlyRevenue(@Param("year") int year);

    @Query(value = """
        SELECT
            c.customer_id,
            c.first_name || ' ' || c.last_name AS name,
            SUM(p.amount)                       AS total_spent,
            COUNT(DISTINCT r.rental_id)         AS total_rentals,
            RANK() OVER (ORDER BY SUM(p.amount) DESC) AS rank
        FROM customer c
        JOIN payment p ON p.customer_id = c.customer_id
        JOIN rental  r ON r.customer_id = c.customer_id
        GROUP BY c.customer_id, name
        ORDER BY total_spent DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopCustomers(@Param("limit") int limit);

    @Query(value = """
        SELECT
            cat.name                AS category,
            COUNT(r.rental_id)      AS total_rentals,
            SUM(p.amount)           AS total_revenue
        FROM category cat
        JOIN film_category fc ON fc.category_id = cat.category_id
        JOIN film f            ON f.film_id      = fc.film_id
        JOIN inventory i       ON i.film_id      = f.film_id
        JOIN rental r          ON r.inventory_id = i.inventory_id
        LEFT JOIN payment p    ON p.rental_id    = r.rental_id
        GROUP BY cat.category_id, cat.name
        ORDER BY total_rentals DESC
        """, nativeQuery = true)
    List<Object[]> findRentalsByCategory();
}
