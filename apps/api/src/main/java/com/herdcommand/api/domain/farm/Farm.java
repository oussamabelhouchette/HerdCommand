package com.herdcommand.api.domain.farm;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "farm")
public class Farm extends AuditedEntity {

    @Column(nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false)
    private boolean active = true;

    protected Farm() {}

    public Farm(String code, String nameAr, String nameEn) {
        this.code = code;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.active = true;
    }

    public String getCode() {
        return code;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public boolean isActive() {
        return active;
    }
}
