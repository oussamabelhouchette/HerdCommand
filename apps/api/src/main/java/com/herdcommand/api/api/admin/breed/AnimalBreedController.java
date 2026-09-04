package com.herdcommand.api.api.admin.breed;

import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.breed.AnimalBreedService;
import com.herdcommand.api.domain.breed.SpeciesCode;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/animal-breeds")
@Tag(name = "Animal breeds")
@SecurityRequirement(name = "bearer-jwt")
public class AnimalBreedController {

    private final AnimalBreedService animalBreedService;

    public AnimalBreedController(AnimalBreedService animalBreedService) {
        this.animalBreedService = animalBreedService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    @Operation(summary = "Search animal breeds")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated breeds"),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PageResponse<BreedResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SpeciesCode speciesCode,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return animalBreedService.search(search, speciesCode, active, pageable);
    }

    @GetMapping("/{breedId}")
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    @Operation(summary = "Get one animal breed")
    public BreedResponse get(@PathVariable UUID breedId) {
        return animalBreedService.get(breedId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BREED_MANAGE')")
    @Operation(summary = "Create an animal breed")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public BreedResponse create(@Valid @RequestBody CreateBreedRequest request) {
        return animalBreedService.create(request);
    }

    @PutMapping("/{breedId}")
    @PreAuthorize("hasAuthority('BREED_MANAGE')")
    @Operation(summary = "Update an animal breed. Code is immutable.")
    public BreedResponse update(@PathVariable UUID breedId, @Valid @RequestBody UpdateBreedRequest request) {
        return animalBreedService.update(breedId, request);
    }

    @PatchMapping("/{breedId}/status")
    @PreAuthorize("hasAuthority('BREED_MANAGE')")
    @Operation(summary = "Activate or deactivate a breed without deleting it")
    public BreedResponse updateStatus(
            @PathVariable UUID breedId,
            @Valid @RequestBody BreedStatusRequest request) {
        return animalBreedService.updateStatus(breedId, request);
    }
}
