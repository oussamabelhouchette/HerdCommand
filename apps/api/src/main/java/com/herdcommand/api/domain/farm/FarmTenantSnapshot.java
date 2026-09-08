package com.herdcommand.api.domain.farm;

import java.util.UUID;

public record FarmTenantSnapshot(
        UUID id,
        String code,
        String nameAr,
        String nameEn,
        String nameFr,
        String governorateCode,
        String address,
        String timezone,
        String defaultLanguage,
        String currencyCode,
        FarmStatus status,
        boolean active,
        long version
) {

    static FarmTenantSnapshot from(Farm farm) {
        return new FarmTenantSnapshot(
                farm.getId(),
                farm.getCode(),
                farm.getNameAr(),
                farm.getNameEn(),
                farm.getNameFr(),
                farm.getGovernorateCode(),
                farm.getAddress(),
                farm.getTimezone(),
                farm.getDefaultLanguage().code(),
                farm.getCurrencyCode(),
                farm.getStatus(),
                farm.isActive(),
                farm.getVersion() == null ? 0L : farm.getVersion());
    }
}
