package com.herdcommand.api.api.farm.animal;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.herdcommand.api.config.BlankUuidDeserializer;
import com.herdcommand.api.domain.animal.GenderCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateAnimalRequest(
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 40, message = "{validation.size}")
        String identificationNumber,
        @Size(max = 100, message = "{validation.size}")
        String name,
        @NotNull(message = "{validation.notNull}")
        UUID breedId,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 40, message = "{validation.size}")
        String statusCode,
        @NotNull(message = "{validation.notNull}")
        GenderCode genderCode,
        @PastOrPresent(message = "{validation.pastOrPresent}")
        LocalDate dateOfBirth,
        @JsonDeserialize(using = BlankUuidDeserializer.class)
        UUID groupId
) {
    public UpdateAnimalRequest {
        identificationNumber = CreateAnimalRequest.trimToEmpty(identificationNumber);
        name = CreateAnimalRequest.blankToNull(name);
        statusCode = statusCode == null ? null : statusCode.trim().toUpperCase();
    }
}
