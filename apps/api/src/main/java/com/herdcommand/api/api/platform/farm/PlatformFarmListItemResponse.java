package com.herdcommand.api.api.platform.farm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlatformFarmListItemResponse(
        UUID id,
        String code,
        String name,
        String ownerDisplayName,
        String ownerEmail,
        String planCode,
        int activeAnimalCount,
        int maxActiveAnimals,
        List<String> enabledFeatureCodes,
        String status,
        Instant createdAt,
        long version
) {
    public PlatformFarmListItemResponse {
        enabledFeatureCodes = enabledFeatureCodes == null ? List.of() : List.copyOf(enabledFeatureCodes);
    }
}
