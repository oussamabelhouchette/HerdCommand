package com.herdcommand.api.domain.farm;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "farm")
public class Farm extends AuditedEntity {

    public static final String DEFAULT_TIMEZONE = "Africa/Tunis";
    public static final String CURRENCY_TND = "TND";

    @Column(nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "name_ar", nullable = false, length = 150)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 150)
    private String nameFr;

    @Column(name = "governorate_code", nullable = false, length = 10)
    private String governorateCode;

    @Column(length = 500)
    private String address;

    @Column(nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    @Convert(converter = FarmLanguageConverter.class)
    @Column(name = "default_language", nullable = false, length = 8)
    private FarmLanguage defaultLanguage = FarmLanguage.AR;

    @Column(name = "currency_code", nullable = false, length = 8, updatable = false)
    private String currencyCode = CURRENCY_TND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FarmStatus status = FarmStatus.SETUP;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Farm() {}

    public Farm(
            String code,
            String nameAr,
            String nameEn,
            String nameFr,
            String governorateCode,
            String address,
            String timezone,
            FarmLanguage defaultLanguage) {
        this.code = code;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.nameFr = nameFr;
        this.governorateCode = governorateCode;
        this.address = address;
        this.timezone = timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : timezone;
        this.defaultLanguage = defaultLanguage == null ? FarmLanguage.AR : defaultLanguage;
        this.currencyCode = CURRENCY_TND;
        applyStatus(FarmStatus.SETUP);
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

    public String getNameFr() {
        return nameFr;
    }

    public void setNameFr(String nameFr) {
        this.nameFr = nameFr;
    }

    public String getGovernorateCode() {
        return governorateCode;
    }

    public void setGovernorateCode(String governorateCode) {
        this.governorateCode = governorateCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public FarmLanguage getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(FarmLanguage defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public FarmStatus getStatus() {
        return status;
    }

    public void applyStatus(FarmStatus next) {
        this.status = next;
        this.active = next == FarmStatus.ACTIVE;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }
}
