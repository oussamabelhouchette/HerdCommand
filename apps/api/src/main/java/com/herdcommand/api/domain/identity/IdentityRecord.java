package com.herdcommand.api.domain.identity;

public record IdentityRecord(
        String keycloakUserId,
        String email,
        String displayName,
        boolean enabled
) {}
