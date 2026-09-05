package com.herdcommand.api.domain.status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ColorTokenConverter implements AttributeConverter<ColorToken, String> {

    @Override
    public String convertToDatabaseColumn(ColorToken attribute) {
        return attribute == null ? null : attribute.token();
    }

    @Override
    public ColorToken convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ColorToken.fromToken(dbData);
    }
}
