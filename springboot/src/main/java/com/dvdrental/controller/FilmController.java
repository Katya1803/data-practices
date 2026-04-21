package com.dvdrental.controller;

import com.dvdrental.dto.ActorDto;
import com.dvdrental.dto.ApiResponse;
import com.dvdrental.dto.FilmDetailDto;
import com.dvdrental.dto.FilmDto;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.service.FilmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/films")
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;

    @GetMapping
    public ApiResponse<PageResponse<FilmDto>> getFilms(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String rating,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(filmService.getFilms(category, rating, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<FilmDetailDto> getFilmById(@PathVariable int id) {
        return ApiResponse.ok(filmService.getFilmById(id));
    }

    @GetMapping("/search")
    public ApiResponse<List<FilmDto>> searchFilms(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(filmService.searchFilms(keyword, pageable));
    }

    @GetMapping("/available")
    public ApiResponse<List<FilmDto>> getAvailableFilms(@RequestParam int storeId) {
        return ApiResponse.ok(filmService.getAvailableFilms(storeId));
    }

    @GetMapping("/{id}/cast")
    public ApiResponse<List<ActorDto>> getFilmCast(@PathVariable int id) {
        return ApiResponse.ok(filmService.getFilmCast(id));
    }
}
