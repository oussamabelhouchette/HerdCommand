package com.herdcommand.api.domain.farm;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "farm_onboarding_request")
public class FarmOnboardingRequest extends AuditedEntity {

    @Column(name = "idempotency_key", nullable = false, length = 80, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FarmOnboardingStatus status = FarmOnboardingStatus.STARTED;

    @Column(name = "farm_id")
    private UUID farmId;

    @Column(name = "created_identity_id", length = 64)
    private String createdIdentityId;

    @Column(name = "response_json", length = 4000)
    private String responseJson;

    protected FarmOnboardingRequest() {}

    public FarmOnboardingRequest(String idempotencyKey, String requestFingerprint) {
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = FarmOnboardingStatus.STARTED;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public FarmOnboardingStatus getStatus() {
        return status;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public String getCreatedIdentityId() {
        return createdIdentityId;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void rememberCreatedIdentity(String keycloakUserId) {
        this.createdIdentityId = keycloakUserId;
    }

    public void complete(UUID farmId, String responseJson) {
        this.status = FarmOnboardingStatus.COMPLETED;
        this.farmId = farmId;
        this.responseJson = responseJson;
    }

    public void fail() {
        this.status = FarmOnboardingStatus.FAILED;
    }

    public void restart() {
        this.status = FarmOnboardingStatus.STARTED;
        this.farmId = null;
        this.createdIdentityId = null;
        this.responseJson = null;
    }
}
