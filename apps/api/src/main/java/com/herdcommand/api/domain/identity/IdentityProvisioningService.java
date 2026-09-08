package com.herdcommand.api.domain.identity;

import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.FarmMembershipStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IdentityProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(IdentityProvisioningService.class);

    private final IdentityDirectory identityDirectory;
    private final IdentityLookupService identityLookupService;

    public IdentityProvisioningService(
            IdentityDirectory identityDirectory, IdentityLookupService identityLookupService) {
        this.identityDirectory = identityDirectory;
        this.identityLookupService = identityLookupService;
    }

    public OwnerResolution resolveOwner(String rawEmail, String displayName) {
        IdentityLookupResult lookup = identityLookupService.lookup(rawEmail);
        if (lookup.found()) {
            if (!Boolean.TRUE.equals(lookup.enabled())) {
                throw new BadRequestException(ErrorCodes.IDENTITY_DISABLED, "error.identity.disabled");
            }
            return new OwnerResolution(
                    lookup.keycloakUserId(),
                    lookup.email(),
                    lookup.displayName(),
                    FarmMembershipStatus.ACTIVE,
                    false,
                    false);
        }

        String email = lookup.email();
        IdentityRecord created;
        try {
            created = identityDirectory.createInvitedUser(email, displayName);
        } catch (IdentityProviderException ex) {
            throw unavailable();
        }

        boolean emailSent = false;
        try {
            identityDirectory.sendExecuteActionsEmail(created.keycloakUserId());
            emailSent = true;
        } catch (IdentityProviderException ex) {
            log.warn("Invitation email failed after creating Keycloak user; invitation remains retryable");
        }

        log.info("Provisioned invited owner membershipStatus=INVITED emailSent={}", emailSent);
        return new OwnerResolution(
                created.keycloakUserId(),
                created.email(),
                created.displayName(),
                FarmMembershipStatus.INVITED,
                emailSent,
                true);
    }

    public void resendInvitation(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            throw new ConflictException(ErrorCodes.IDENTITY_NOT_FOUND, "error.identity.notFound");
        }
        try {
            identityDirectory.sendExecuteActionsEmail(keycloakUserId.trim());
        } catch (IdentityProviderException ex) {
            throw unavailable();
        }
        log.info("Resent owner invitation email");
    }

    public void compensateCreatedIdentity(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return;
        }
        try {
            identityDirectory.deleteCreatedIdentity(keycloakUserId);
            log.info("Compensated unused Keycloak identity created by this operation");
        } catch (IdentityProviderException ex) {
            log.warn("Compensation delete of unused Keycloak identity failed; record for retry");
            throw unavailable();
        }
    }

    private static ApiException unavailable() {
        return new ApiException(
                ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "error.identity.unavailable");
    }
}
