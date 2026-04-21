package com.dvdrental.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MpaaRating {
    G, PG, PG_13, R, NC_17;

    @JsonCreator
    public static MpaaRating fromString(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase().replace("-", "_")) {
            case "G"     -> G;
            case "PG"    -> PG;
            case "PG_13" -> PG_13;
            case "R"     -> R;
            case "NC_17" -> NC_17;
            default -> throw new IllegalArgumentException("Unknown rating: " + value);
        };
    }

    @JsonValue
    public String toDbValue() {
        return this.name().replace("_", "-");
    }
}
