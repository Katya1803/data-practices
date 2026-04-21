package com.dvdrental.controller;

import com.dvdrental.dto.ApiResponse;
import com.dvdrental.dto.CategoryRentalDto;
import com.dvdrental.dto.MonthlyRevenueDto;
import com.dvdrental.dto.TopCustomerDto;
import com.dvdrental.dto.TopFilmDto;
import com.dvdrental.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/films/top")
    public ApiResponse<List<TopFilmDto>> getTopFilms(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(reportService.getTopFilms(limit));
    }

    @GetMapping("/revenue/monthly")
    public ApiResponse<List<MonthlyRevenueDto>> getMonthlyRevenue(
            @RequestParam(defaultValue = "2005") int year) {
        return ApiResponse.ok(reportService.getMonthlyRevenue(year));
    }

    @GetMapping("/customers/top")
    public ApiResponse<List<TopCustomerDto>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(reportService.getTopCustomers(limit));
    }

    @GetMapping("/rentals/by-category")
    public ApiResponse<List<CategoryRentalDto>> getRentalsByCategory() {
        return ApiResponse.ok(reportService.getRentalsByCategory());
    }
}
