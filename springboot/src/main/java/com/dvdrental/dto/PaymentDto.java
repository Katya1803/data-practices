package com.dvdrental.dto;

import com.dvdrental.model.Payment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class PaymentDto {
    private Integer paymentId;
    private Integer customerId;
    private Integer rentalId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;

    public PaymentDto(Payment p) {
        this.paymentId = p.getPaymentId();
        this.customerId = p.getCustomer().getCustomerId();
        this.rentalId = p.getRental().getRentalId();
        this.amount = p.getAmount();
        this.paymentDate = p.getPaymentDate();
    }
}
