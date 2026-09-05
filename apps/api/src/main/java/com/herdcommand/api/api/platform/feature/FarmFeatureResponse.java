package com.herdcommand.api.api.platform.feature;

import com.herdcommand.api.domain.feature.FarmFeature;

import java.time.Instant;
import java.util.UUID;

public record FarmFeatureResponse(
        UUID id,
        UUID farmId,
        UUID featureId,
        String featureCode,
        boolean enabled,
        Instant enabledAt,
        String enabledBy,
        String configurationJson
) {

    public static FarmFeatureResponse from(FarmFeature assignment, String featureCode) {
        return new FarmFeatureResponse(
                assignment.getId(),
                assignment.getFarmId(),
                assignment.getFeatureId(),
                featureCode,
                assignment.isEnabled(),
                assignment.getEnabledAt(),
                assignment.getEnabledBy(),
                assignment.getConfigurationJson());
    }
}
