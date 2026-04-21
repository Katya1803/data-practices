package com.dvdrental.controller;

import com.dvdrental.dto.ApiResponse;
import com.dvdrental.dto.CustomerDto;
import com.dvdrental.dto.CustomerStatsDto;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.dto.RentalDto;
import com.dvdrental.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResponse<CustomerDto>> getCustomers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(customerService.getCustomers(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDto> getCustomerById(@PathVariable int id) {
        return ApiResponse.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/{id}/rentals")
    public ApiResponse<PageResponse<RentalDto>> getCustomerRentals(
            @PathVariable int id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(customerService.getCustomerRentals(id, pageable));
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<CustomerStatsDto> getCustomerStats(@PathVariable int id) {
        return ApiResponse.ok(customerService.getCustomerStats(id));
    }

    @GetMapping("/overdue")
    public ApiResponse<List<CustomerDto>> getOverdueCustomers() {
        return ApiResponse.ok(customerService.getOverdueCustomers());
    }
}
