package com.herdcommand.api.domain.farm;

public record CreateFarmCommand(
        String nameAr,
        String nameEn,
        String nameFr,
        String governorateCode,
        String address,
        String timezone,
        String defaultLanguage
) {}
