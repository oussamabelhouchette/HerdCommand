package com.herdcommand.api.api.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        String path,
        List<FieldError> errors
) {
    public record FieldError(String field, String message, String code) {}

    public static ApiError of(String code, String message, String correlationId, String path) {
        return new ApiError(code, message, correlationId, Instant.now(), path, List.of());
    }
}
