package com.herdcommand.api.api.admin.breed;

import com.herdcommand.api.domain.breed.SpeciesCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBreedRequest(
        String code,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameAr,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameEn,
        @NotNull(message = "{validation.notNull}")
        SpeciesCode speciesCode,
        @NotNull(message = "{validation.notNull}")
        @Min(value = 0, message = "{validation.min}")
        Integer displayOrder
) {
    public UpdateBreedRequest {
        code = code == null ? null : CreateBreedRequest.normalizeCode(code);
        nameAr = CreateBreedRequest.trimToNull(nameAr);
        nameEn = CreateBreedRequest.trimToNull(nameEn);
    }
}
