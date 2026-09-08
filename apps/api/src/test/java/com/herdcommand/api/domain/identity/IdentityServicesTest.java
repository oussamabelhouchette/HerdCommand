package com.herdcommand.api.domain.identity;

import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.FarmMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityServicesTest {

    private IdentityDirectory directory;
    private IdentityLookupService lookupService;
    private IdentityProvisioningService provisioningService;

    @BeforeEach
    void setUp() {
        directory = mock(IdentityDirectory.class);
        lookupService = new IdentityLookupService(directory);
        provisioningService = new IdentityProvisioningService(directory, lookupService);
    }

    @Test
    void lookupReturnsExistingEnabledUser() {
        when(directory.findByExactEmail("owner@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-1", "owner@example.tn", "Mohamed Ben Salem", true)));

        IdentityLookupResult result = lookupService.lookup("  Owner@Example.TN ");

        assertThat(result.found()).isTrue();
        assertThat(result.invitationRequired()).isFalse();
        assertThat(result.keycloakUserId()).isEqualTo("kc-1");
        assertThat(result.displayName()).isEqualTo("Mohamed Ben Salem");
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void lookupMissingUserRequiresInvitation() {
        when(directory.findByExactEmail("new@example.tn")).thenReturn(List.of());

        IdentityLookupResult result = lookupService.lookup("new@example.tn");

        assertThat(result.found()).isFalse();
        assertThat(result.invitationRequired()).isTrue();
        assertThat(result.email()).isEqualTo("new@example.tn");
        verify(directory, never()).createInvitedUser(any(), any());
    }

    @Test
    void lookupReturnsDisabledWithoutCreating() {
        when(directory.findByExactEmail("off@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-off", "off@example.tn", "Off", false)));

        IdentityLookupResult result = lookupService.lookup("off@example.tn");

        assertThat(result.found()).isTrue();
        assertThat(result.enabled()).isFalse();
        verify(directory, never()).createInvitedUser(any(), any());
    }

    @Test
    void lookupUnavailableBecomesServiceError() {
        when(directory.findByExactEmail("owner@example.tn"))
                .thenThrow(new IdentityProviderException("down"));

        assertThatThrownBy(() -> lookupService.lookup("owner@example.tn"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE);
    }

    @Test
    void resolveOwnerRejectsDisabledIdentity() {
        when(directory.findByExactEmail("off@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-off", "off@example.tn", "Off", false)));

        assertThatThrownBy(() -> provisioningService.resolveOwner("off@example.tn", "Off"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.IDENTITY_DISABLED);
        verify(directory, never()).createInvitedUser(any(), any());
    }

    @Test
    void resolveOwnerUsesExistingEnabledUser() {
        when(directory.findByExactEmail("owner@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-1", "owner@example.tn", "Mohamed", true)));

        OwnerResolution resolved = provisioningService.resolveOwner("Owner@Example.TN", "Ignored");

        assertThat(resolved.keycloakUserId()).isEqualTo("kc-1");
        assertThat(resolved.membershipStatus()).isEqualTo(FarmMembershipStatus.ACTIVE);
        assertThat(resolved.invitationEmailSent()).isFalse();
        assertThat(resolved.identityCreated()).isFalse();
        verify(directory, never()).createInvitedUser(any(), any());
        verify(directory, never()).sendExecuteActionsEmail(any());
    }

    @Test
    void resolveOwnerInvitesMissingUser() {
        when(directory.findByExactEmail("new@example.tn")).thenReturn(List.of());
        when(directory.createInvitedUser("new@example.tn", "New Owner"))
                .thenReturn(new IdentityRecord("kc-new", "new@example.tn", "New Owner", true));

        OwnerResolution resolved = provisioningService.resolveOwner("new@example.tn", "New Owner");

        assertThat(resolved.membershipStatus()).isEqualTo(FarmMembershipStatus.INVITED);
        assertThat(resolved.invitationEmailSent()).isTrue();
        assertThat(resolved.identityCreated()).isTrue();
        verify(directory).sendExecuteActionsEmail("kc-new");
    }

    @Test
    void invitationEmailFailureStaysRetryable() {
        when(directory.findByExactEmail("new@example.tn")).thenReturn(List.of());
        when(directory.createInvitedUser(eq("new@example.tn"), any()))
                .thenReturn(new IdentityRecord("kc-new", "new@example.tn", "New", true));
        org.mockito.Mockito.doThrow(new IdentityProviderException("smtp"))
                .when(directory).sendExecuteActionsEmail("kc-new");

        OwnerResolution resolved = provisioningService.resolveOwner("new@example.tn", "New");

        assertThat(resolved.membershipStatus()).isEqualTo(FarmMembershipStatus.INVITED);
        assertThat(resolved.invitationEmailSent()).isFalse();
    }
}
