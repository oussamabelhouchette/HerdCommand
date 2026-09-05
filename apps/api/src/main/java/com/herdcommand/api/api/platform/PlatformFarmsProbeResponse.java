package com.herdcommand.api.api.platform;

import java.util.List;

/**
 * Security probe for {@code GET /api/v1/platform/farms}. The real farm list is PA-006.
 * This body never includes farm records.
 */
public record PlatformFarmsProbeResponse(
        boolean accessible,
        List<Object> farms
) {
    public static PlatformFarmsProbeResponse empty() {
        return new PlatformFarmsProbeResponse(true, List.of());
    }
}
