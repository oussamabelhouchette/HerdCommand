package com.herdcommand.auditprobe;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "jpa_audit_probe")
public class JpaAuditProbe extends AuditedEntity {

    @Column(nullable = false, length = 100)
    private String name;

    protected JpaAuditProbe() {}

    public JpaAuditProbe(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
