package com.dvdrental.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RentalReturnedEvent {
    private Integer rentalId;
    private Integer customerId;
    private Integer filmId;
    private LocalDateTime returnDate;
    private Long daysRented;
}
