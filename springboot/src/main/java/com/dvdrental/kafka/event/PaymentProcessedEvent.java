package com.dvdrental.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private Integer paymentId;
    private Integer rentalId;
    private Integer customerId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
}
