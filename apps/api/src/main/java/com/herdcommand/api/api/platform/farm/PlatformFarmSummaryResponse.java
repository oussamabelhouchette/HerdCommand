package com.herdcommand.api.api.platform.farm;

public record PlatformFarmSummaryResponse(
        long totalFarms,
        long activeFarms,
        long setupFarms,
        long suspendedFarms,
        long archivedFarms,
        long activeAnimals
) {}
