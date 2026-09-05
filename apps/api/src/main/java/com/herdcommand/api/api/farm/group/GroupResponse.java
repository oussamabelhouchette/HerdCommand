package com.herdcommand.api.api.farm.group;

import com.herdcommand.api.domain.group.AnimalGroup;
import com.herdcommand.api.domain.group.GroupTypeCode;

import java.time.Instant;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        UUID farmId,
        String code,
        String nameAr,
        String nameEn,
        GroupTypeCode groupTypeCode,
        String description,
        Integer capacity,
        boolean active,
        int animalCount,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
    public static GroupResponse from(AnimalGroup group) {
        return new GroupResponse(
                group.getId(),
                group.getFarmId(),
                group.getCode(),
                group.getNameAr(),
                group.getNameEn(),
                group.getGroupTypeCode(),
                group.getDescription(),
                group.getCapacity(),
                group.isActive(),
                0,
                group.getCreatedAt(),
                group.getCreatedBy(),
                group.getUpdatedAt(),
                group.getUpdatedBy());
    }
}
