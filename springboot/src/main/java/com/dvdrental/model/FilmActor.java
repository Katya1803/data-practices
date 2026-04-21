package com.dvdrental.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "film_actor")
@Getter @Setter
public class FilmActor {

    @EmbeddedId
    private FilmActorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("actorId")
    @JoinColumn(name = "actor_id")
    private Actor actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("filmId")
    @JoinColumn(name = "film_id")
    private Film film;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}
