package com.dvdrental.service;

import com.dvdrental.dto.RealtimeStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RealtimeStatsService {

    private final StringRedisTemplate redis;

    private static final String KEY_RENTALS  = "stats:rentals:";
    private static final String KEY_RETURNS  = "stats:returns:";
    private static final String KEY_PAYMENTS = "stats:payments:";
    private static final String KEY_REVENUE  = "stats:revenue:";

    public void incrementRentals(LocalDate date) {
        redis.opsForValue().increment(KEY_RENTALS + date);
    }

    public void incrementReturns(LocalDate date) {
        redis.opsForValue().increment(KEY_RETURNS + date);
    }

    public void recordPayment(LocalDate date, BigDecimal amount) {
        redis.opsForValue().increment(KEY_PAYMENTS + date);
        redis.opsForValue().increment(KEY_REVENUE + date, amount.doubleValue());
    }

    public RealtimeStatsDto getStats(LocalDate date) {
        String rentals  = redis.opsForValue().get(KEY_RENTALS  + date);
        String returns  = redis.opsForValue().get(KEY_RETURNS  + date);
        String payments = redis.opsForValue().get(KEY_PAYMENTS + date);
        String revenue  = redis.opsForValue().get(KEY_REVENUE  + date);

        RealtimeStatsDto dto = new RealtimeStatsDto();
        dto.setDate(date.toString());
        dto.setRentalsCreated   (rentals  != null ? Long.parseLong(rentals)           : 0L);
        dto.setRentalsReturned  (returns  != null ? Long.parseLong(returns)           : 0L);
        dto.setPaymentsProcessed(payments != null ? Long.parseLong(payments)          : 0L);
        dto.setTotalRevenue     (revenue  != null ? new BigDecimal(revenue)           : BigDecimal.ZERO);
        return dto;
    }
}
