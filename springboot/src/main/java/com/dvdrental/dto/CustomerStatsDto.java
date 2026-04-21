package com.dvdrental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class CustomerStatsDto {
    private Integer customerId;
    private String firstName;
    private String lastName;
    private Long totalRentals;
    private BigDecimal totalSpent;
    private BigDecimal avgPayment;

    public CustomerStatsDto(Object[] row) {
        this.customerId   = ((Number) row[0]).intValue();
        this.firstName    = (String) row[1];
        this.lastName     = (String) row[2];
        this.totalRentals = ((Number) row[3]).longValue();
        this.totalSpent   = new BigDecimal(row[4].toString());
        this.avgPayment   = new BigDecimal(row[5].toString());
    }
}
