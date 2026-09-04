package com.herdcommand.api.support;

import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;

import jakarta.validation.Valid;

@Profile("test")
@RestController
@RequestMapping("/api/v1/admin/animal-config")
public class AnimalConfigProbeController {

    @PostMapping("/validation-check")
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validate(@Valid @RequestBody ProbeRequest request) {
        // Foundation-only probe so validation serialization can be asserted.
    }

    @GetMapping("/not-found-check")
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    public void notFound() {
        throw new ResourceNotFoundException();
    }

    @GetMapping("/conflict-check")
    @PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
    public void conflict() {
        throw new ConflictException(ErrorCodes.BREED_CODE_ALREADY_EXISTS, "error.breed.codeExists");
    }

    public record ProbeRequest(
            @NotBlank(message = "{validation.notBlank}") String code
    ) {}
}
