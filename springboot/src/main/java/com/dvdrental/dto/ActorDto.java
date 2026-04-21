package com.dvdrental.dto;

import com.dvdrental.model.Actor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ActorDto {
    private Integer actorId;
    private String firstName;
    private String lastName;

    public ActorDto(Actor actor) {
        this.actorId = actor.getActorId();
        this.firstName = actor.getFirstName();
        this.lastName = actor.getLastName();
    }
}
