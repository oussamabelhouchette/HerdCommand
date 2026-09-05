package com.herdcommand.api.domain.feature;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "feature_catalog")
public class FeatureCatalog extends AuditedEntity {

    @Column(nullable = false, length = 60, updatable = false)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 150)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 150)
    private String nameFr;

    @Column(name = "description_ar", length = 500)
    private String descriptionAr;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description_fr", length = 500)
    private String descriptionFr;

    @Column(name = "icon_code", length = 40)
    private String iconCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_status", nullable = false, length = 20)
    private FeatureReleaseStatus releaseStatus;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected FeatureCatalog() {}

    public FeatureCatalog(
            String code,
            String nameAr,
            String nameEn,
            String nameFr,
            FeatureReleaseStatus releaseStatus,
            int displayOrder) {
        this.code = code;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.nameFr = nameFr;
        this.releaseStatus = releaseStatus;
        this.displayOrder = displayOrder;
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

    public String getNameFr() {
        return nameFr;
    }

    public String getDescriptionAr() {
        return descriptionAr;
    }

    public void setDescriptionAr(String descriptionAr) {
        this.descriptionAr = descriptionAr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public void setDescriptionFr(String descriptionFr) {
        this.descriptionFr = descriptionFr;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }

    public FeatureReleaseStatus getReleaseStatus() {
        return releaseStatus;
    }

    public void setReleaseStatus(FeatureReleaseStatus releaseStatus) {
        this.releaseStatus = releaseStatus;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEnableable() {
        return active && releaseStatus == FeatureReleaseStatus.AVAILABLE;
    }

    public String localizedName(String language) {
        return switch (language) {
            case "fr" -> nameFr;
            case "en" -> nameEn;
            default -> nameAr;
        };
    }

    public String localizedDescription(String language) {
        return switch (language) {
            case "fr" -> descriptionFr;
            case "en" -> descriptionEn;
            default -> descriptionAr;
        };
    }
}
