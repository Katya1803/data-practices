package com.dvdrental.dto;

import com.dvdrental.model.Rental;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class RentalDto {
    private Integer rentalId;
    private LocalDateTime rentalDate;
    private LocalDateTime returnDate;
    private Integer customerId;
    private String customerName;
    private Integer inventoryId;
    private Integer filmId;
    private String filmTitle;
    private Integer storeId;

    public RentalDto(Rental r) {
        this.rentalId = r.getRentalId();
        this.rentalDate = r.getRentalDate();
        this.returnDate = r.getReturnDate();
        this.customerId = r.getCustomer().getCustomerId();
        this.customerName = r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName();
        this.inventoryId = r.getInventory().getInventoryId();
        this.filmId = r.getInventory().getFilm().getFilmId();
        this.filmTitle = r.getInventory().getFilm().getTitle();
        this.storeId = (int) r.getInventory().getStoreId();
    }
}
