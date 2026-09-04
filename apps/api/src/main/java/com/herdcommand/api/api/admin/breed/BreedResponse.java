package com.herdcommand.api.api.admin.breed;

import com.herdcommand.api.domain.breed.AnimalBreed;
import com.herdcommand.api.domain.breed.SpeciesCode;

import java.time.Instant;
import java.util.UUID;

public record BreedResponse(
        UUID id,
        String code,
        String nameAr,
        String nameEn,
        SpeciesCode speciesCode,
        int displayOrder,
        boolean active,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
    public static BreedResponse from(AnimalBreed breed) {
        return new BreedResponse(
                breed.getId(),
                breed.getCode(),
                breed.getNameAr(),
                breed.getNameEn(),
                breed.getSpeciesCode(),
                breed.getDisplayOrder(),
                breed.isActive(),
                breed.getCreatedAt(),
                breed.getCreatedBy(),
                breed.getUpdatedAt(),
                breed.getUpdatedBy());
    }
}
