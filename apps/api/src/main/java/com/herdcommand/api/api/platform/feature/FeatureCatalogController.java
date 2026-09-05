package com.herdcommand.api.api.platform.feature;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.feature.FarmFeatureService;
import com.herdcommand.api.domain.feature.FeatureCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform")
@Tag(name = "Platform administration")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class FeatureCatalogController {

    private final FeatureCatalogService featureCatalogService;
    private final FarmFeatureService farmFeatureService;

    public FeatureCatalogController(
            FeatureCatalogService featureCatalogService, FarmFeatureService farmFeatureService) {
        this.featureCatalogService = featureCatalogService;
        this.farmFeatureService = farmFeatureService;
    }

    @GetMapping("/features")
    @Operation(summary = "List released features from the catalog")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catalog items"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FeatureCatalogResponse list(
            @RequestParam(name = "includeComingSoon", defaultValue = "false") boolean includeComingSoon,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return featureCatalogService.list(includeComingSoon, acceptLanguage);
    }

    @PostMapping("/farms/{farmId}/features")
    @Operation(summary = "Enable or disable a catalog feature for a farm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment saved"),
            @ApiResponse(responseCode = "400", description = "Feature is not available",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Farm or feature not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FarmFeatureResponse assign(
            @PathVariable UUID farmId, @Valid @RequestBody AssignFarmFeatureRequest request) {
        return farmFeatureService.assign(farmId, request);
    }
}
