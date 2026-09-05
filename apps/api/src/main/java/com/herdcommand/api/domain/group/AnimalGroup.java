package com.herdcommand.api.domain.group;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "animal_group")
public class AnimalGroup extends AuditedEntity {

    @Column(name = "farm_id", nullable = false, updatable = false)
    private UUID farmId;

    @Column(nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type_code", nullable = false, length = 30)
    private GroupTypeCode groupTypeCode;

    @Column(length = 500)
    private String description;

    @Column
    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;

    protected AnimalGroup() {}

    public AnimalGroup(
            UUID farmId,
            String code,
            String nameAr,
            String nameEn,
            GroupTypeCode groupTypeCode,
            String description,
            Integer capacity) {
        this.farmId = farmId;
        this.code = code;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.groupTypeCode = groupTypeCode;
        this.description = description;
        this.capacity = capacity;
        this.active = true;
    }

    public UUID getFarmId() {
        return farmId;
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

    public GroupTypeCode getGroupTypeCode() {
        return groupTypeCode;
    }

    public void setGroupTypeCode(GroupTypeCode groupTypeCode) {
        this.groupTypeCode = groupTypeCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
