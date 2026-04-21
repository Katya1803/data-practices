package com.dvdrental.dto;

import com.dvdrental.model.Film;
import com.dvdrental.model.MpaaRating;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class FilmDto {
    private Integer filmId;
    private String title;
    private String description;
    private Integer releaseYear;
    private Short rentalDuration;
    private BigDecimal rentalRate;
    private Short length;
    private BigDecimal replacementCost;
    private MpaaRating rating;

    public FilmDto(Film film) {
        this.filmId = film.getFilmId();
        this.title = film.getTitle();
        this.description = film.getDescription();
        this.releaseYear = film.getReleaseYear();
        this.rentalDuration = film.getRentalDuration();
        this.rentalRate = film.getRentalRate();
        this.length = film.getLength();
        this.replacementCost = film.getReplacementCost();
        this.rating = film.getRating();
    }
}
