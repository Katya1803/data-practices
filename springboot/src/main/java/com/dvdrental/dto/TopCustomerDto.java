package com.dvdrental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class TopCustomerDto {
    private Integer customerId;
    private String name;
    private BigDecimal totalSpent;
    private Long totalRentals;
    private Long rank;

    public TopCustomerDto(Object[] row) {
        this.customerId = ((Number) row[0]).intValue();
        this.name = (String) row[1];
        this.totalSpent = new BigDecimal(row[2].toString());
        this.totalRentals = ((Number) row[3]).longValue();
        this.rank = ((Number) row[4]).longValue();
    }
}
