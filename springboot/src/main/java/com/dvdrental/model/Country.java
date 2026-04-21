package com.dvdrental.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "country")
@Getter @Setter
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Integer countryId;

    @Column(name = "country", nullable = false, length = 50)
    private String country;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}
