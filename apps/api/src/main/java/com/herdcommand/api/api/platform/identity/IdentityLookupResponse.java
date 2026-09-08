package com.herdcommand.api.api.platform.identity;

import com.herdcommand.api.domain.identity.IdentityLookupResult;

public record IdentityLookupResponse(
        boolean found,
        boolean invitationRequired,
        String keycloakUserId,
        String email,
        String displayName,
        Boolean enabled
) {

    public static IdentityLookupResponse from(IdentityLookupResult result) {
        return new IdentityLookupResponse(
                result.found(),
                result.invitationRequired(),
                result.keycloakUserId(),
                result.email(),
                result.displayName(),
                result.enabled());
    }
}
