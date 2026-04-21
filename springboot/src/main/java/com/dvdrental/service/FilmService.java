package com.dvdrental.service;

import com.dvdrental.config.CacheConfig;
import com.dvdrental.dto.ActorDto;
import com.dvdrental.dto.FilmDetailDto;
import com.dvdrental.dto.FilmDto;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.model.Film;
import com.dvdrental.model.MpaaRating;
import com.dvdrental.repository.FilmRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilmService {

    private final FilmRepository filmRepository;

    public PageResponse<FilmDto> getFilms(String category, String rating, Pageable pageable) {
        // Normalize rating to DB format (e.g. PG-13, NC-17)
        String ratingDb = rating != null ? MpaaRating.fromString(rating).toDbValue() : null;
        Page<Film> page;
        if (category != null && ratingDb != null) {
            page = filmRepository.findByCategoryAndRating(category, ratingDb, pageable);
        } else if (category != null) {
            page = filmRepository.findByCategory(category, pageable);
        } else if (ratingDb != null) {
            page = filmRepository.findByRating(ratingDb, pageable);
        } else {
            page = filmRepository.findAll(pageable);
        }
        return new PageResponse<>(page.map(FilmDto::new));
    }

    @Cacheable(value = CacheConfig.CACHE_FILM, key = "#id")
    public FilmDetailDto getFilmById(int id) {
        Film film = filmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Film not found: " + id));
        return new FilmDetailDto(film);
    }

    public List<FilmDto> searchFilms(String keyword, Pageable pageable) {
        return filmRepository.fullTextSearch(keyword, pageable)
                .stream().map(FilmDto::new).toList();
    }

    @Cacheable(value = CacheConfig.CACHE_FILM_AVAILABLE, key = "#storeId")
    public ArrayList<FilmDto> getAvailableFilms(int storeId) {
        return filmRepository.findAvailableByStore(storeId).stream()
                .map(FilmDto::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public List<ActorDto> getFilmCast(int filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new EntityNotFoundException("Film not found: " + filmId));
        return new ArrayList<>(film.getActors().stream().map(ActorDto::new).toList());
    }
}
