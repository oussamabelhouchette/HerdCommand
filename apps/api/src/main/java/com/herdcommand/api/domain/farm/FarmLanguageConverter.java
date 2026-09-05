package com.herdcommand.api.domain.farm;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class FarmLanguageConverter implements AttributeConverter<FarmLanguage, String> {

    @Override
    public String convertToDatabaseColumn(FarmLanguage attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public FarmLanguage convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FarmLanguage.fromCode(dbData);
    }
}
