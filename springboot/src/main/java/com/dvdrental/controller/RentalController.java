package com.dvdrental.controller;

import com.dvdrental.dto.ApiResponse;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.dto.RentalDto;
import com.dvdrental.dto.RentalRequest;
import com.dvdrental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @GetMapping("/active")
    public ApiResponse<PageResponse<RentalDto>> getActiveRentals(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(rentalService.getActiveRentals(pageable));
    }

    @GetMapping("/overdue")
    public ApiResponse<List<RentalDto>> getOverdueRentals() {
        return ApiResponse.ok(rentalService.getOverdueRentals());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RentalDto> createRental(@RequestBody RentalRequest request) {
        return ApiResponse.ok(rentalService.createRental(request));
    }

    @PutMapping("/{id}/return")
    public ApiResponse<RentalDto> returnRental(@PathVariable int id) {
        return ApiResponse.ok(rentalService.returnRental(id));
    }
}
