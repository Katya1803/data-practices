package com.dvdrental.repository;

import com.dvdrental.model.Film;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FilmRepository extends JpaRepository<Film, Integer> {

    Page<Film> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query(value = """
        SELECT f.* FROM film f
        WHERE to_tsvector('english', f.title || ' ' || COALESCE(f.description, ''))
              @@ plainto_tsquery('english', :keyword)
        """, nativeQuery = true)
    List<Film> fullTextSearch(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT DISTINCT f FROM Film f JOIN f.categories c
        WHERE LOWER(c.name) = LOWER(:category)
        """)
    Page<Film> findByCategory(@Param("category") String category, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT f.* FROM film f
        JOIN film_category fc ON fc.film_id = f.film_id
        JOIN category c ON c.category_id = fc.category_id
        WHERE LOWER(c.name) = LOWER(:category) AND CAST(f.rating AS text) = :rating
        """,
        countQuery = """
        SELECT COUNT(DISTINCT f.film_id) FROM film f
        JOIN film_category fc ON fc.film_id = f.film_id
        JOIN category c ON c.category_id = fc.category_id
        WHERE LOWER(c.name) = LOWER(:category) AND CAST(f.rating AS text) = :rating
        """,
        nativeQuery = true)
    Page<Film> findByCategoryAndRating(@Param("category") String category,
                                       @Param("rating") String rating,
                                       Pageable pageable);

    @Query(value = "SELECT f.* FROM film f WHERE CAST(f.rating AS text) = :rating",
           countQuery = "SELECT COUNT(*) FROM film f WHERE CAST(f.rating AS text) = :rating",
           nativeQuery = true)
    Page<Film> findByRating(@Param("rating") String rating, Pageable pageable);

    @Query(value = """
        SELECT DISTINCT f.* FROM film f
        JOIN inventory i ON i.film_id = f.film_id
        WHERE i.store_id = :storeId
          AND i.inventory_id NOT IN (
              SELECT r.inventory_id FROM rental r WHERE r.return_date IS NULL
          )
        """, nativeQuery = true)
    List<Film> findAvailableByStore(@Param("storeId") int storeId);
}
