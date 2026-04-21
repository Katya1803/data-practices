package com.dvdrental.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class PaymentRequest {
    private Integer rentalId;
    private Integer customerId;
    private Integer staffId;
    private BigDecimal amount;
}
