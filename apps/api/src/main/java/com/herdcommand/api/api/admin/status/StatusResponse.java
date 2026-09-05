package com.herdcommand.api.api.admin.status;

import com.herdcommand.api.domain.status.AnimalStatusDefinition;
import com.herdcommand.api.domain.status.ColorToken;

import java.time.Instant;

public record StatusResponse(
        String code,
        String labelAr,
        String labelEn,
        ColorToken colorToken,
        int displayOrder,
        boolean visibleInFilter,
        boolean active,
        boolean systemProtected,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy
) {
    public static StatusResponse from(AnimalStatusDefinition status) {
        return new StatusResponse(
                status.getCode(),
                status.getLabelAr(),
                status.getLabelEn(),
                status.getColorToken(),
                status.getDisplayOrder(),
                status.isVisibleInFilter(),
                status.isActive(),
                status.isSystemProtected(),
                status.getCreatedAt(),
                status.getCreatedBy(),
                status.getUpdatedAt(),
                status.getUpdatedBy());
    }
}
