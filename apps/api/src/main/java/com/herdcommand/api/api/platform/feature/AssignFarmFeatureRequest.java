package com.herdcommand.api.api.platform.feature;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignFarmFeatureRequest(
        @NotNull(message = "{validation.notNull}") UUID featureId,
        @NotNull(message = "{validation.notNull}") Boolean enabled,
        String configurationJson
) {}
