package com.dvdrental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class RealtimeStatsDto {
    private String date;
    private Long rentalsCreated;
    private Long rentalsReturned;
    private Long paymentsProcessed;
    private BigDecimal totalRevenue;
}
