package com.herdcommand.api.domain.farm;

import java.util.List;

public record ValidatedFarmOnboarding(
        CreateFarmCommand farm,
        String ownerEmail,
        String ownerDisplayName,
        FarmPlanLimits.Applied subscription,
        List<String> enabledFeatureCodes
) {
    public ValidatedFarmOnboarding {
        enabledFeatureCodes = enabledFeatureCodes == null ? List.of() : List.copyOf(enabledFeatureCodes);
    }
}
