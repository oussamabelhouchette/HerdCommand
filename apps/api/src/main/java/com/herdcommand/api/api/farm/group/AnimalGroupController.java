package com.herdcommand.api.api.farm.group;

import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.domain.group.AnimalGroupService;
import com.herdcommand.api.domain.group.GroupTypeCode;
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
@RequestMapping("/api/v1/farms/{farmId}/animal-groups")
@Tag(name = "Animal groups")
@SecurityRequirement(name = "bearer-jwt")
public class AnimalGroupController {

    private final AnimalGroupService animalGroupService;

    public AnimalGroupController(AnimalGroupService animalGroupService) {
        this.animalGroupService = animalGroupService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROUP_VIEW')")
    @Operation(summary = "Search farm groups")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated groups"),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PageResponse<GroupResponse> list(
            @PathVariable UUID farmId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) GroupTypeCode groupTypeCode,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "nameAr", direction = Sort.Direction.ASC) Pageable pageable) {
        return animalGroupService.search(farmId, search, groupTypeCode, active, pageable);
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasAuthority('GROUP_VIEW')")
    @Operation(summary = "Get one farm group")
    public GroupResponse get(@PathVariable UUID farmId, @PathVariable UUID groupId) {
        return animalGroupService.get(farmId, groupId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    @Operation(summary = "Create a farm group")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public GroupResponse create(@PathVariable UUID farmId, @Valid @RequestBody CreateGroupRequest request) {
        return animalGroupService.create(farmId, request);
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    @Operation(summary = "Update a farm group. Code is immutable.")
    public GroupResponse update(
            @PathVariable UUID farmId, @PathVariable UUID groupId, @Valid @RequestBody UpdateGroupRequest request) {
        return animalGroupService.update(farmId, groupId, request);
    }

    @PatchMapping("/{groupId}/status")
    @PreAuthorize("hasAuthority('GROUP_MANAGE')")
    @Operation(summary = "Activate or deactivate a group without deleting it")
    public GroupResponse updateStatus(
            @PathVariable UUID farmId,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupStatusRequest request) {
        return animalGroupService.updateStatus(farmId, groupId, request);
    }
}
