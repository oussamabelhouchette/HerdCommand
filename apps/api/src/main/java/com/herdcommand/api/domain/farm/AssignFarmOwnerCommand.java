package com.herdcommand.api.domain.farm;

import java.util.UUID;

public record AssignFarmOwnerCommand(
        UUID farmId,
        String keycloakUserId,
        String invitedEmail
) {}
