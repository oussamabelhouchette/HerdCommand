package com.herdcommand.api.api.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IdentityLookupRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Email(message = "{validation.email}")
        String email
) {
    public IdentityLookupRequest {
        email = email == null ? null : email.trim();
    }
}
