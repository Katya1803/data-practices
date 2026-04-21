package com.dvdrental.dto;

import com.dvdrental.model.Customer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CustomerDto {
    private Integer customerId;
    private String firstName;
    private String lastName;
    private String email;
    private Short storeId;
    private Boolean active;

    public CustomerDto(Customer c) {
        this.customerId = c.getCustomerId();
        this.firstName = c.getFirstName();
        this.lastName = c.getLastName();
        this.email = c.getEmail();
        this.storeId = c.getStoreId();
        this.active = c.getActivebool();
    }
}
