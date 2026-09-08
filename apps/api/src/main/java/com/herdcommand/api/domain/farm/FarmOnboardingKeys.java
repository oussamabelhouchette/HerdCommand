package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ErrorCodes;

import java.util.List;
import java.util.UUID;

public final class FarmOnboardingKeys {

    private FarmOnboardingKeys() {}

    public static UUID require(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED, "error.idempotency.keyRequired");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    ErrorCodes.IDEMPOTENCY_KEY_REQUIRED,
                    "error.idempotency.keyRequired",
                    List.of(new ApiError.FieldError("Idempotency-Key", "invalid")));
        }
    }
}
