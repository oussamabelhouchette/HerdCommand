package com.herdcommand.api.api.admin.status;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String labelAr,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String labelEn,
        @NotBlank(message = "{validation.notBlank}")
        String colorToken,
        @NotNull(message = "{validation.notNull}")
        @Min(value = 0, message = "{validation.min}")
        Integer displayOrder,
        @NotNull(message = "{validation.notNull}")
        Boolean visibleInFilter,
        @NotNull(message = "{validation.notNull}")
        Boolean active
) {
    public UpdateStatusRequest {
        labelAr = trimToNull(labelAr);
        labelEn = trimToNull(labelEn);
        colorToken = colorToken == null ? null : colorToken.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
