package com.herdcommand.api.api.platform.farm;

import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.farm.PlatformFarmQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/farms")
@Tag(name = "Platform administration")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class PlatformFarmQueryController {

    private final PlatformFarmQueryService platformFarmQueryService;

    public PlatformFarmQueryController(PlatformFarmQueryService platformFarmQueryService) {
        this.platformFarmQueryService = platformFarmQueryService;
    }

    @GetMapping
    @Operation(summary = "Search and page all farms")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Farm page"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or sort",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PageResponse<PlatformFarmListItemResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String featureCode,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return platformFarmQueryService.search(search, status, planCode, featureCode, acceptLanguage, pageable);
    }

    @GetMapping("/summary")
    @Operation(summary = "Return farm counts for the platform dashboard")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregate counts"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PlatformFarmSummaryResponse summary() {
        return platformFarmQueryService.summary();
    }

    @GetMapping("/{farmId}")
    @Operation(summary = "Return one farm for platform support")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Farm details"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing PLATFORM_ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Farm not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PlatformFarmDetailsResponse get(
            @PathVariable UUID farmId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return platformFarmQueryService.get(farmId, acceptLanguage);
    }
}
