package com.dvdrental.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MpaaRatingConverter implements AttributeConverter<MpaaRating, String> {

    @Override
    public String convertToDatabaseColumn(MpaaRating attribute) {
        return attribute == null ? null : attribute.toDbValue();
    }

    @Override
    public MpaaRating convertToEntityAttribute(String dbData) {
        return MpaaRating.fromString(dbData);
    }
}
