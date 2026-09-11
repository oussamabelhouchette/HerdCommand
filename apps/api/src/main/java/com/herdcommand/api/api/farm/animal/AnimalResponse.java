package com.herdcommand.api.api.farm.animal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalResponse(
        UUID id,
        UUID farmId,
        String identificationNumber,
        String name,
        AnimalBreedRef breed,
        String genderCode,
        LocalDate dateOfBirth,
        String ageDisplay,
        AnimalGroupRef group,
        AnimalStatusRef status,
        Instant createdAt,
        Instant updatedAt
) {
    public record AnimalBreedRef(UUID id, String code, String nameAr, String nameEn) {}

    public record AnimalGroupRef(UUID id, String nameAr, String nameEn) {}

    public record AnimalStatusRef(String code, String nameAr, String nameEn, String colorToken) {}
}
