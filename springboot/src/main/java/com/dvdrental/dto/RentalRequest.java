package com.dvdrental.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RentalRequest {
    private Integer inventoryId;
    private Integer customerId;
    private Integer staffId;
}
