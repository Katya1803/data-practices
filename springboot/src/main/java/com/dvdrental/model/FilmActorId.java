package com.dvdrental.model;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Embeddable
@Getter @Setter @EqualsAndHashCode
public class FilmActorId implements Serializable {
    private Integer actorId;
    private Integer filmId;
}
