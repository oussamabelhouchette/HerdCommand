package com.herdcommand.api.api.admin.breed;

import com.herdcommand.api.domain.breed.SpeciesCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateBreedRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 40, message = "{validation.size}")
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]{0,39}$", message = "{validation.breed.codePattern}")
        String code,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameAr,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameEn,
        @NotNull(message = "{validation.notNull}")
        SpeciesCode speciesCode,
        @Min(value = 0, message = "{validation.min}")
        Integer displayOrder
) {
    public CreateBreedRequest {
        code = normalizeCode(code);
        nameAr = trimToNull(nameAr);
        nameEn = trimToNull(nameEn);
        displayOrder = displayOrder == null ? 0 : displayOrder;
    }

    static String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? "" : normalized;
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
