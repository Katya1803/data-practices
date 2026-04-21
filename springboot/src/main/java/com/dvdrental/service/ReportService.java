package com.dvdrental.service;

import com.dvdrental.config.CacheConfig;
import com.dvdrental.dto.CategoryRentalDto;
import com.dvdrental.dto.MonthlyRevenueDto;
import com.dvdrental.dto.TopCustomerDto;
import com.dvdrental.dto.TopFilmDto;
import com.dvdrental.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    @Cacheable(value = CacheConfig.CACHE_REPORT_TOP_FILMS, key = "#limit")
    public ArrayList<TopFilmDto> getTopFilms(int limit) {
        return reportRepository.findTopFilms(limit).stream()
                .map(TopFilmDto::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(value = CacheConfig.CACHE_REPORT_REVENUE, key = "#year")
    public ArrayList<MonthlyRevenueDto> getMonthlyRevenue(int year) {
        return reportRepository.findMonthlyRevenue(year).stream()
                .map(MonthlyRevenueDto::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(value = CacheConfig.CACHE_REPORT_CUSTOMERS, key = "#limit")
    public ArrayList<TopCustomerDto> getTopCustomers(int limit) {
        return reportRepository.findTopCustomers(limit).stream()
                .map(TopCustomerDto::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Cacheable(value = CacheConfig.CACHE_REPORT_CATEGORY)
    public ArrayList<CategoryRentalDto> getRentalsByCategory() {
        return reportRepository.findRentalsByCategory().stream()
                .map(CategoryRentalDto::new)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
