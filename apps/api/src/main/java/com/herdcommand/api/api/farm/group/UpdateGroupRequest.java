package com.herdcommand.api.api.farm.group;

import com.herdcommand.api.domain.group.GroupTypeCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        String code,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameAr,
        @NotBlank(message = "{validation.notBlank}")
        @Size(max = 100, message = "{validation.size}")
        String nameEn,
        @NotNull(message = "{validation.notNull}")
        GroupTypeCode groupTypeCode,
        @Size(max = 500, message = "{validation.size}")
        String description,
        @Min(value = 1, message = "{validation.min}")
        Integer capacity
) {
    public UpdateGroupRequest {
        code = code == null ? null : CreateGroupRequest.normalizeCode(code);
        nameAr = CreateGroupRequest.trimToNull(nameAr);
        nameEn = CreateGroupRequest.trimToNull(nameEn);
        description = CreateGroupRequest.blankToNull(description);
    }
}
