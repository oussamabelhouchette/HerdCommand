package com.herdcommand.api.domain.identity;

public record IdentityLookupResult(
        boolean found,
        boolean invitationRequired,
        String keycloakUserId,
        String email,
        String displayName,
        Boolean enabled
) {

    public static IdentityLookupResult existing(IdentityRecord record) {
        return new IdentityLookupResult(
                true, false, record.keycloakUserId(), record.email(), record.displayName(), record.enabled());
    }

    public static IdentityLookupResult invitationRequired(String email) {
        return new IdentityLookupResult(false, true, null, email, null, null);
    }
}
