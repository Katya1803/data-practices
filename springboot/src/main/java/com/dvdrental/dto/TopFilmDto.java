package com.dvdrental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class TopFilmDto {
    private Integer filmId;
    private String title;
    private Long rentalCount;

    public TopFilmDto(Object[] row) {
        this.filmId = ((Number) row[0]).intValue();
        this.title = (String) row[1];
        this.rentalCount = ((Number) row[2]).longValue();
    }
}
