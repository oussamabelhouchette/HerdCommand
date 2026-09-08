package com.herdcommand.api.api.me;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.farm.OwnerFarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/farms")
@Tag(name = "Identity")
@SecurityRequirement(name = "bearer-jwt")
public class OwnerFarmController {

    private final OwnerFarmService ownerFarmService;

    public OwnerFarmController(OwnerFarmService ownerFarmService) {
        this.ownerFarmService = ownerFarmService;
    }

    @GetMapping
    @Operation(summary = "List farms the caller owns")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned farms"),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<OwnerFarmResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return ownerFarmService.list(jwt.getSubject(), acceptLanguage);
    }
}
