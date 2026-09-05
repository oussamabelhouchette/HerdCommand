package com.herdcommand.api.api.admin.status;

import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.status.AnimalStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/animal-statuses")
@Tag(name = "Animal statuses")
@SecurityRequirement(name = "bearer-jwt")
public class AnimalStatusController {

    private final AnimalStatusService animalStatusService;

    public AnimalStatusController(AnimalStatusService animalStatusService) {
        this.animalStatusService = animalStatusService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    @Operation(summary = "Search animal status definitions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated statuses"),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PageResponse<StatusResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return animalStatusService.search(search, active, pageable);
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    @Operation(summary = "Get one animal status definition")
    public StatusResponse get(@PathVariable String code) {
        return animalStatusService.get(code);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('STATUS_CONFIG_MANAGE')")
    @Operation(summary = "Update status presentation. Technical code is immutable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public StatusResponse update(@PathVariable String code, @Valid @RequestBody UpdateStatusRequest request) {
        return animalStatusService.update(code, request);
    }

    @PatchMapping("/{code}/status")
    @PreAuthorize("hasAuthority('STATUS_CONFIG_MANAGE')")
    @Operation(summary = "Activate or deactivate a status without deleting it")
    public StatusResponse updateStatus(
            @PathVariable String code,
            @Valid @RequestBody StatusActiveRequest request) {
        return animalStatusService.updateStatus(code, request);
    }
}
