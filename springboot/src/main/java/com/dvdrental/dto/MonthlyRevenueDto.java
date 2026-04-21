package com.dvdrental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class MonthlyRevenueDto {
    private Integer month;
    private BigDecimal revenue;
    private Long transactionCount;

    public MonthlyRevenueDto(Object[] row) {
        this.month = ((Number) row[0]).intValue();
        this.revenue = new BigDecimal(row[1].toString());
        this.transactionCount = ((Number) row[2]).longValue();
    }
}
