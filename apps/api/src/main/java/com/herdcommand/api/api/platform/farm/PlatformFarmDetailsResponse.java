package com.herdcommand.api.api.platform.farm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlatformFarmDetailsResponse(
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
        String ownerDisplayName,
        String ownerEmail,
        String ownerMembershipStatus,
        String planCode,
        int activeAnimalCount,
        int maxActiveAnimals,
        Integer maxTeamMembers,
        Instant trialEndsAt,
        List<String> enabledFeatureCodes,
        Instant createdAt,
        String createdBy,
        long version
) {
    public PlatformFarmDetailsResponse {
        enabledFeatureCodes = enabledFeatureCodes == null ? List.of() : List.copyOf(enabledFeatureCodes);
    }
}
