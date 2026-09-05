package com.herdcommand.api.api.platform.identity;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.identity.IdentityLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/identity")
@Tag(name = "Platform administration")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class IdentityController {

    private final IdentityLookupService identityLookupService;

    public IdentityController(IdentityLookupService identityLookupService) {
        this.identityLookupService = identityLookupService;
    }

    @PostMapping("/lookup")
    @Operation(summary = "Look up a farm owner in Keycloak by exact email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found identity or invitation required"),
            @ApiResponse(responseCode = "400", description = "Invalid email",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Identity provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public IdentityLookupResponse lookup(@Valid @RequestBody IdentityLookupRequest request) {
        return IdentityLookupResponse.from(identityLookupService.lookup(request.email()));
    }
}
