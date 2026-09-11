package com.herdcommand.api.api.farm.animal;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.animal.AnimalService;
import com.herdcommand.api.domain.animal.GenderCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/animals")
@Tag(name = "Animals")
@SecurityRequirement(name = "bearer-jwt")
public class AnimalController {

    private final AnimalService animalService;

    public AnimalController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping("/lookups")
    @PreAuthorize("hasAuthority('ANIMAL_VIEW')")
    @Operation(summary = "Reference data for the animals list of an owned farm")
    public AnimalLookupResponse lookups(
            @PathVariable UUID farmId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return animalService.lookups(farmId, acceptLanguage);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANIMAL_VIEW')")
    @Operation(summary = "Search animals on an owned farm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated animals"),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AnimalPageResponse list(
            @PathVariable UUID farmId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID breedId,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) GenderCode gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return animalService.search(farmId, search, breedId, statusCode, groupId, gender, page, acceptLanguage);
    }

    @GetMapping("/{animalId}")
    @PreAuthorize("hasAuthority('ANIMAL_VIEW')")
    public AnimalResponse get(
            @PathVariable UUID farmId,
            @PathVariable UUID animalId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return animalService.get(farmId, animalId, acceptLanguage);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ANIMAL_MANAGE')")
    @Operation(summary = "Register an animal on an owned farm")
    public AnimalResponse create(
            @PathVariable UUID farmId,
            @Valid @RequestBody CreateAnimalRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return animalService.create(farmId, request, acceptLanguage);
    }

    @PutMapping("/{animalId}")
    @PreAuthorize("hasAuthority('ANIMAL_MANAGE')")
    public AnimalResponse update(
            @PathVariable UUID farmId,
            @PathVariable UUID animalId,
            @Valid @RequestBody UpdateAnimalRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return animalService.update(farmId, animalId, request, acceptLanguage);
    }

    @DeleteMapping("/{animalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ANIMAL_MANAGE')")
    public void archive(@PathVariable UUID farmId, @PathVariable UUID animalId) {
        animalService.archive(farmId, animalId);
    }
}
