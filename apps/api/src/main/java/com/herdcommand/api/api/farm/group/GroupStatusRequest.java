package com.herdcommand.api.api.farm.group;

import jakarta.validation.constraints.NotNull;

public record GroupStatusRequest(@NotNull(message = "{validation.notNull}") Boolean active) {}
