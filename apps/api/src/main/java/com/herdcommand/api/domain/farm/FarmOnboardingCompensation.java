package com.herdcommand.api.domain.farm;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "farm_onboarding_compensation")
public class FarmOnboardingCompensation extends AuditedEntity {

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "keycloak_user_id", nullable = false, length = 64)
    private String keycloakUserId;

    @Column(length = 200)
    private String reason;

    @Column(nullable = false)
    private boolean resolved;

    protected FarmOnboardingCompensation() {}

    public FarmOnboardingCompensation(String idempotencyKey, String keycloakUserId, String reason) {
        this.idempotencyKey = idempotencyKey;
        this.keycloakUserId = keycloakUserId;
        this.reason = reason;
        this.resolved = false;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public String getReason() {
        return reason;
    }

    public boolean isResolved() {
        return resolved;
    }
}
