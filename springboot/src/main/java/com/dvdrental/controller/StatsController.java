package com.dvdrental.controller;

import com.dvdrental.dto.ApiResponse;
import com.dvdrental.dto.RealtimeStatsDto;
import com.dvdrental.service.RealtimeStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final RealtimeStatsService statsService;

    @GetMapping("/realtime")
    public ApiResponse<RealtimeStatsDto> getRealtimeStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(statsService.getStats(date != null ? date : LocalDate.now()));
    }
}
