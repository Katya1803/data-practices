package com.dvdrental.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "language")
@Getter @Setter
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "language_id")
    private Integer languageId;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}
