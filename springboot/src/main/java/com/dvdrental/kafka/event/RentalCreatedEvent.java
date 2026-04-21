package com.dvdrental.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RentalCreatedEvent {
    private Integer rentalId;
    private Integer customerId;
    private Integer filmId;
    private Integer inventoryId;
    private LocalDateTime rentalDate;
}
