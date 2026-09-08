package com.herdcommand.api.api.platform.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubscriptionPayload(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 30, message = "{validation.size}")
        String planCode,
        Integer maxActiveAnimals,
        Integer maxTeamMembers,
        @Size(max = 32, message = "{validation.size}")
        String trialEndsAt
) {
    public SubscriptionPayload {
        planCode = planCode == null ? null : planCode.trim();
        trialEndsAt = trialEndsAt == null || trialEndsAt.isBlank() ? null : trialEndsAt.trim();
    }
}
