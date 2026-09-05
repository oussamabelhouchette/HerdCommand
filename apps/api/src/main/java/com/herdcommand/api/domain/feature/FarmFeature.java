package com.herdcommand.api.domain.feature;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farm_feature")
public class FarmFeature extends AuditedEntity {

    @Column(name = "farm_id", nullable = false, updatable = false)
    private UUID farmId;

    @Column(name = "feature_id", nullable = false, updatable = false)
    private UUID featureId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "enabled_at")
    private Instant enabledAt;

    @Column(name = "enabled_by", length = 64)
    private String enabledBy;

    @Column(name = "configuration_json", length = 4000)
    private String configurationJson;

    protected FarmFeature() {}

    public FarmFeature(UUID farmId, UUID featureId) {
        this.farmId = farmId;
        this.featureId = featureId;
        this.enabled = false;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public UUID getFeatureId() {
        return featureId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getEnabledAt() {
        return enabledAt;
    }

    public String getEnabledBy() {
        return enabledBy;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void enable(String auditor, String configurationJson, Instant when) {
        this.enabled = true;
        this.enabledAt = when;
        this.enabledBy = auditor;
        this.configurationJson = configurationJson;
    }

    public void disable() {
        this.enabled = false;
    }

    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }
}
