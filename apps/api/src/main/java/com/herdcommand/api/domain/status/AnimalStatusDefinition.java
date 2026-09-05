package com.herdcommand.api.domain.status;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "animal_status_definition")
@EntityListeners(AuditingEntityListener.class)
public class AnimalStatusDefinition {

    public static final String REQUIRED_CODE = "ACTIVE";

    @Id
    @Column(nullable = false, updatable = false, length = 40)
    private String code;

    @Column(name = "label_ar", nullable = false, length = 100)
    private String labelAr;

    @Column(name = "label_en", nullable = false, length = 100)
    private String labelEn;

    @Convert(converter = ColorTokenConverter.class)
    @Column(name = "color_token", nullable = false, length = 30)
    private ColorToken colorToken;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "visible_in_filter", nullable = false)
    private boolean visibleInFilter = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_protected", nullable = false)
    private boolean systemProtected = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 64)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    protected AnimalStatusDefinition() {}

    public AnimalStatusDefinition(
            String code,
            String labelAr,
            String labelEn,
            ColorToken colorToken,
            int displayOrder,
            boolean visibleInFilter,
            boolean active,
            boolean systemProtected) {
        this.code = code;
        this.labelAr = labelAr;
        this.labelEn = labelEn;
        this.colorToken = colorToken;
        this.displayOrder = displayOrder;
        this.visibleInFilter = visibleInFilter;
        this.active = active;
        this.systemProtected = systemProtected;
    }

    public boolean isRequiredLifecycleStatus() {
        return REQUIRED_CODE.equals(code);
    }

    public String getCode() {
        return code;
    }

    public String getLabelAr() {
        return labelAr;
    }

    public void setLabelAr(String labelAr) {
        this.labelAr = labelAr;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public void setLabelEn(String labelEn) {
        this.labelEn = labelEn;
    }

    public ColorToken getColorToken() {
        return colorToken;
    }

    public void setColorToken(ColorToken colorToken) {
        this.colorToken = colorToken;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isVisibleInFilter() {
        return visibleInFilter;
    }

    public void setVisibleInFilter(boolean visibleInFilter) {
        this.visibleInFilter = visibleInFilter;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSystemProtected() {
        return systemProtected;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
