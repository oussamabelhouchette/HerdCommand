package com.herdcommand.api.api.platform.farm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePlatformFarmRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 150, message = "{validation.size}")
        String nameAr,
        @Size(max = 150, message = "{validation.size}")
        String nameEn,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 150, message = "{validation.size}")
        String nameFr,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 10, message = "{validation.size}")
        String governorateCode,
        @Size(max = 500, message = "{validation.size}")
        String address,
        @Size(max = 64, message = "{validation.size}")
        String timezone,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 8, message = "{validation.size}")
        String defaultLanguage,
        @Size(max = 8, message = "{validation.size}")
        String currencyCode,
        @Size(max = 20, message = "{validation.size}")
        String initialStatus,
        @NotNull(message = "{validation.notNull}")
        @Valid
        OwnerPayload owner,
        @NotNull(message = "{validation.notNull}")
        @Valid
        SubscriptionPayload subscription,
        List<@NotBlank(message = "{validation.notBlank}") @Size(max = 60, message = "{validation.size}") String> enabledFeatureCodes
) {
    public CreatePlatformFarmRequest {
        nameAr = trimToNull(nameAr);
        nameEn = trimToNull(nameEn);
        nameFr = trimToNull(nameFr);
        governorateCode = trimToNull(governorateCode);
        address = trimToNull(address);
        timezone = trimToNull(timezone);
        defaultLanguage = trimToNull(defaultLanguage);
        currencyCode = trimToNull(currencyCode);
        initialStatus = trimToNull(initialStatus);
        enabledFeatureCodes = enabledFeatureCodes == null ? List.of() : List.copyOf(enabledFeatureCodes);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
