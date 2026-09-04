package com.herdcommand.api.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ApiError", description = "Standard API error envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @Schema(example = "BREED_CODE_ALREADY_EXISTS") String code,
        @Schema(example = "A breed with this code already exists.") String message,
        List<FieldError> fieldErrors,
        Instant timestamp,
        @Schema(example = "/api/v1/admin/animal-breeds") String path
) {
    public record FieldError(
            @Schema(example = "code") String field,
            @Schema(example = "Code must be unique.") String message
    ) {}

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, List.of(), Instant.now(), path);
    }

    public static ApiError of(String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(code, message, fieldErrors == null ? List.of() : fieldErrors, Instant.now(), path);
    }
}
