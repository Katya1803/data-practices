package com.dvdrental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class CategoryRentalDto {
    private String category;
    private Long totalRentals;
    private BigDecimal totalRevenue;

    public CategoryRentalDto(Object[] row) {
        this.category = (String) row[0];
        this.totalRentals = ((Number) row[1]).longValue();
        this.totalRevenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
    }
}
