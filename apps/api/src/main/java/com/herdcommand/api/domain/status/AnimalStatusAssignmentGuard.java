package com.herdcommand.api.domain.status;

import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalStatusAssignmentGuard {

    private final AnimalStatusService animalStatusService;

    public AnimalStatusAssignmentGuard(AnimalStatusService animalStatusService) {
        this.animalStatusService = animalStatusService;
    }

    @Transactional(readOnly = true)
    public AnimalStatusDefinition requireAssignable(String code) {
        AnimalStatusDefinition status = animalStatusService.requireStatus(code);
        if (!status.isActive()) {
            throw new ConflictException(ErrorCodes.STATUS_INACTIVE, "error.status.inactive");
        }
        return status;
    }
}
