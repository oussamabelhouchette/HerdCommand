package com.herdcommand.api.api.farm;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.farm.FarmService;
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
@RequestMapping("/api/v1/farms")
@Tag(name = "Farms")
@SecurityRequirement(name = "bearer-jwt")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROUP_VIEW')")
    @Operation(summary = "List farms the caller may use")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Farms"),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<FarmResponse> list() {
        return farmService.list();
    }
}
