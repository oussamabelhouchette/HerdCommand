package com.herdcommand.api.domain.breed;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "animal_breed")
public class AnimalBreed extends AuditedEntity {

    @Column(nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "species_code", nullable = false, length = 30)
    private SpeciesCode speciesCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected AnimalBreed() {}

    public AnimalBreed(
            String code,
            String nameAr,
            String nameEn,
            SpeciesCode speciesCode,
            int displayOrder) {
        this.code = code;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.speciesCode = speciesCode;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    public String getCode() {
        return code;
    }

    public String getNameAr() {
        return nameAr;
    }

    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public SpeciesCode getSpeciesCode() {
        return speciesCode;
    }

    public void setSpeciesCode(SpeciesCode speciesCode) {
        this.speciesCode = speciesCode;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
