package com.herdcommand.api.domain.identity;

import java.util.List;

public interface IdentityDirectory {

    List<IdentityRecord> findByExactEmail(String normalizedEmail);

    IdentityRecord createInvitedUser(String normalizedEmail, String displayName);

    void sendExecuteActionsEmail(String keycloakUserId);

    void deleteCreatedIdentity(String keycloakUserId);
}
