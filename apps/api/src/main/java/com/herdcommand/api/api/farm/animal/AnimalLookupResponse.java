package com.herdcommand.api.api.farm.animal;

import java.util.List;
import java.util.UUID;

public record AnimalLookupResponse(
        List<LookupBreed> breeds,
        List<LookupStatus> statuses,
        List<LookupGroup> groups,
        List<LookupGender> genders
) {
    public record LookupBreed(UUID id, String code, String nameAr, String nameEn, boolean active) {}

    public record LookupStatus(String code, String nameAr, String nameEn, String colorToken, boolean active) {}

    public record LookupGroup(UUID id, String nameAr, String nameEn, boolean active) {}

    public record LookupGender(String code, String nameAr, String nameEn) {}
}
