package com.herdcommand.api.api.platform.farm;

import java.util.List;
import java.util.UUID;

public record PlatformFarmCreatedResponse(
        UUID id,
        String code,
        String status,
        String ownerMembershipStatus,
        boolean invitationEmailSent,
        List<String> enabledFeatureCodes
) {
    public PlatformFarmCreatedResponse {
        enabledFeatureCodes = enabledFeatureCodes == null ? List.of() : List.copyOf(enabledFeatureCodes);
    }
}
