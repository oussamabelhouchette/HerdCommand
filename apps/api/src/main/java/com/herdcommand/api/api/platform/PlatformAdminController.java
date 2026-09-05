package com.herdcommand.api.api.platform;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform")
@Tag(name = "Platform administration")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class PlatformAdminController {

    @GetMapping
    @Operation(summary = "Return platform-admin security foundation metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foundation metadata"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PlatformFoundationResponse foundation() {
        return new PlatformFoundationResponse(
                "ar",
                List.of("ar", "en"),
                List.of(Permission.PLATFORM_ADMIN.name()));
    }

}
