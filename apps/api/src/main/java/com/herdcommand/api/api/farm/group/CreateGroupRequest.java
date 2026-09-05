package com.herdcommand.api.api.farm.group;

import com.herdcommand.api.domain.group.GroupTypeCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateGroupRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 40, message = "{validation.size}")
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]{0,39}$", message = "{validation.group.codePattern}")
        String code,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameAr,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameEn,
        @NotNull(message = "{validation.notNull}")
        GroupTypeCode groupTypeCode,
        @Size(max = 500, message = "{validation.size}")
        String description,
        @Min(value = 1, message = "{validation.min}")
        Integer capacity
) {
    public CreateGroupRequest {
        code = normalizeCode(code);
        nameAr = trimToNull(nameAr);
        nameEn = trimToNull(nameEn);
        description = blankToNull(description);
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

    static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
