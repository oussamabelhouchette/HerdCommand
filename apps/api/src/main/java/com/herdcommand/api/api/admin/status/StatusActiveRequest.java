package com.herdcommand.api.api.admin.status;

import jakarta.validation.constraints.NotNull;

public record StatusActiveRequest(
        @NotNull(message = "{validation.notNull}") Boolean active
) {}
