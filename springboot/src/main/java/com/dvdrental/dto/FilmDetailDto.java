package com.dvdrental.dto;

import com.dvdrental.model.Category;
import com.dvdrental.model.Film;
import com.dvdrental.model.MpaaRating;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class FilmDetailDto {
    private Integer filmId;
    private String title;
    private String description;
    private Integer releaseYear;
    private String language;
    private Short rentalDuration;
    private BigDecimal rentalRate;
    private Short length;
    private BigDecimal replacementCost;
    private MpaaRating rating;
    private List<String> categories;
    private List<ActorDto> cast;

    public FilmDetailDto(Film film) {
        this.filmId = film.getFilmId();
        this.title = film.getTitle();
        this.description = film.getDescription();
        this.releaseYear = film.getReleaseYear();
        this.language = film.getLanguage() != null ? film.getLanguage().getName().trim() : null;
        this.rentalDuration = film.getRentalDuration();
        this.rentalRate = film.getRentalRate();
        this.length = film.getLength();
        this.replacementCost = film.getReplacementCost();
        this.rating = film.getRating();
        this.categories = film.getCategories().stream().map(Category::getName).toList();
        this.cast = film.getActors().stream().map(ActorDto::new).toList();
    }
}
