package com.herdcommand.api.api.me;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OwnerFarmResponse(
        UUID id,
        String code,
        String name,
        String nameAr,
        String nameEn,
        String nameFr,
        String governorateCode,
        String address,
        String timezone,
        String defaultLanguage,
        String currencyCode,
        String status,
        boolean active,
        String planCode,
        int maxActiveAnimals,
        List<String> enabledFeatureCodes,
        boolean animalManagementEnabled,
        Instant createdAt,
        long version
) {
    public static final String ANIMAL_MANAGEMENT = "ANIMAL_MANAGEMENT";

    public OwnerFarmResponse {
        enabledFeatureCodes = enabledFeatureCodes == null ? List.of() : List.copyOf(enabledFeatureCodes);
    }
}
