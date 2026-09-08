package com.herdcommand.api.domain.identity;

import com.herdcommand.api.domain.farm.FarmMembershipStatus;

public record OwnerResolution(
        String keycloakUserId,
        String email,
        String displayName,
        FarmMembershipStatus membershipStatus,
        boolean invitationEmailSent,
        boolean identityCreated
) {}
