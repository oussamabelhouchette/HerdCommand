package com.herdcommand.api.api.admin.breed;

import jakarta.validation.constraints.NotNull;

public record BreedStatusRequest(
        @NotNull(message = "{validation.notNull}") Boolean active
) {}
