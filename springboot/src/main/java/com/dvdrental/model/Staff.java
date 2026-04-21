package com.dvdrental.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff")
@Getter @Setter
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "first_name", nullable = false, length = 45)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 45)
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "store_id", nullable = false)
    private Short storeId;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "username", nullable = false, length = 16)
    private String username;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}
