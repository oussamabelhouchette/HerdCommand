package com.herdcommand.api.domain.farm;

import java.util.UUID;

public record UpdateFarmIdentityCommand(
        UUID farmId,
        long expectedVersion,
        String nameAr,
        String nameEn,
        String nameFr,
        String governorateCode,
        String address,
        String timezone,
        String defaultLanguage
) {}
