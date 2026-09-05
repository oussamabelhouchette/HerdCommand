package com.herdcommand.api.api.platform.farm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerPayload(
        @NotBlank(message = "{validation.notBlank}")
        @Email(message = "{validation.email}")
        @Size(max = 320, message = "{validation.size}")
        String email,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 150, message = "{validation.size}")
        String displayName,
        @Size(max = 20, message = "{validation.size}")
        @Pattern(regexp = "^$|^\\+[1-9][0-9]{7,14}$", message = "{validation.phone}")
        String phoneNumber
) {
    public OwnerPayload {
        email = email == null ? null : email.trim();
        displayName = displayName == null ? null : displayName.trim();
        phoneNumber = blankToNull(phoneNumber);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
