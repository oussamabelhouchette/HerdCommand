package com.herdcommand.api.domain.farm;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farm_subscription")
public class FarmSubscription extends AuditedEntity {

    @Column(name = "farm_id", nullable = false, updatable = false)
    private UUID farmId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 30)
    private FarmPlanCode planCode;

    @Column(name = "max_active_animals", nullable = false)
    private int maxActiveAnimals;

    @Column(name = "max_team_members", nullable = false)
    private int maxTeamMembers;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FarmSubscriptionStatus status = FarmSubscriptionStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private Long version;

    protected FarmSubscription() {}

    public FarmSubscription(
            UUID farmId,
            FarmPlanCode planCode,
            int maxActiveAnimals,
            int maxTeamMembers,
            Instant trialEndsAt) {
        this.farmId = farmId;
        this.planCode = planCode;
        this.maxActiveAnimals = maxActiveAnimals;
        this.maxTeamMembers = maxTeamMembers;
        this.trialEndsAt = trialEndsAt;
        this.status = FarmSubscriptionStatus.ACTIVE;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public FarmPlanCode getPlanCode() {
        return planCode;
    }

    public int getMaxActiveAnimals() {
        return maxActiveAnimals;
    }

    public int getMaxTeamMembers() {
        return maxTeamMembers;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public FarmSubscriptionStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }
}
