package com.herdcommand.api.api.admin.animalconfig;

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

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/animal-config")
@Tag(name = "Animal configuration")
@SecurityRequirement(name = "bearer-jwt")
public class AnimalConfigController {

    @GetMapping
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    @Operation(summary = "Return animal configuration foundation metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foundation metadata"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing permission",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AnimalConfigFoundationResponse foundation() {
        return new AnimalConfigFoundationResponse(
                "ar",
                List.of("ar", "en"),
                Arrays.stream(Permission.values()).map(Enum::name).toList());
    }
}
