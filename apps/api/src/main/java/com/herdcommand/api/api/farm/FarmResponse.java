package com.herdcommand.api.api.farm;

import com.herdcommand.api.domain.farm.Farm;

import java.util.UUID;

public record FarmResponse(UUID id, String code, String nameAr, String nameEn, boolean active) {

    public static FarmResponse from(Farm farm) {
        return new FarmResponse(farm.getId(), farm.getCode(), farm.getNameAr(), farm.getNameEn(), farm.isActive());
    }
}
