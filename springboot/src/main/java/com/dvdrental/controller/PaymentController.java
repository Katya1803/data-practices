package com.dvdrental.controller;

import com.dvdrental.dto.ApiResponse;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.dto.PaymentDto;
import com.dvdrental.dto.PaymentRequest;
import com.dvdrental.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/customer/{customerId}")
    public ApiResponse<PageResponse<PaymentDto>> getCustomerPayments(
            @PathVariable int customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(paymentService.getCustomerPayments(customerId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentDto> createPayment(@RequestBody PaymentRequest request) {
        return ApiResponse.ok(paymentService.createPayment(request));
    }
}
