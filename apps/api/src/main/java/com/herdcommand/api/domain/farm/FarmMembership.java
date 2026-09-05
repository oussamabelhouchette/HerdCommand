package com.herdcommand.api.domain.farm;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "farm_membership")
public class FarmMembership extends AuditedEntity {

    @Column(name = "farm_id", nullable = false, updatable = false)
    private UUID farmId;

    @Column(name = "keycloak_user_id", nullable = false, length = 64, updatable = false)
    private String keycloakUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 30)
    private FarmMembershipRole roleCode = FarmMembershipRole.FARM_OWNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FarmMembershipStatus status;

    @Column(name = "invited_email", nullable = false, length = 320)
    private String invitedEmail;

    @Column(name = "invited_at")
    private Instant invitedAt;

    @Column(name = "invited_by", length = 64)
    private String invitedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected FarmMembership() {}

    public FarmMembership(
            UUID farmId,
            String keycloakUserId,
            String invitedEmail,
            Instant invitedAt,
            String invitedBy,
            FarmMembershipStatus status) {
        this.farmId = farmId;
        this.keycloakUserId = keycloakUserId;
        this.roleCode = FarmMembershipRole.FARM_OWNER;
        this.invitedEmail = normalizeEmail(invitedEmail);
        this.invitedAt = invitedAt;
        this.invitedBy = invitedBy;
        applyStatus(status, Instant.now());
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getFarmId() {
        return farmId;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public FarmMembershipRole getRoleCode() {
        return roleCode;
    }

    public FarmMembershipStatus getStatus() {
        return status;
    }

    public void applyStatus(FarmMembershipStatus next, Instant when) {
        this.status = next;
        if (next == FarmMembershipStatus.ACTIVE && this.acceptedAt == null) {
            this.acceptedAt = when;
        }
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public boolean isActiveOwner() {
        return roleCode == FarmMembershipRole.FARM_OWNER && status == FarmMembershipStatus.ACTIVE;
    }
}
