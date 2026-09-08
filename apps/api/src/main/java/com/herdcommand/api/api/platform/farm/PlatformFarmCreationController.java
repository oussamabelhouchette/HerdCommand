package com.herdcommand.api.api.platform.farm;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.farm.FarmOnboardingKeys;
import com.herdcommand.api.domain.farm.FarmOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform")
@Tag(name = "Platform administration")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class PlatformFarmCreationController {

    private final FarmOnboardingService farmOnboardingService;

    public PlatformFarmCreationController(FarmOnboardingService farmOnboardingService) {
        this.farmOnboardingService = farmOnboardingService;
    }

    @PostMapping("/farms")
    @Operation(summary = "Create a farm, owner membership, subscription and features atomically")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Farm created",
                    headers = @Header(name = "Location", description = "Created farm path")),
            @ApiResponse(responseCode = "400", description = "Validation or unavailable feature",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Identity provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PlatformFarmCreatedResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePlatformFarmRequest request) {
        UUID key = FarmOnboardingKeys.require(idempotencyKey);
        PlatformFarmCreatedResponse created = farmOnboardingService.create(key, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
