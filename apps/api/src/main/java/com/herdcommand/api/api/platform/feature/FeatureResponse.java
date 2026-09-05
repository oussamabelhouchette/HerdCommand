package com.herdcommand.api.api.platform.feature;

import java.util.UUID;

public record FeatureResponse(
        UUID id,
        String code,
        String name,
        String description,
        String iconCode,
        String releaseStatus,
        boolean enableable,
        int displayOrder
) {}
