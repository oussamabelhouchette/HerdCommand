package com.herdcommand.api.domain.breed;

import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnimalBreedAssignmentGuard {

    private final AnimalBreedRepository repository;

    public AnimalBreedAssignmentGuard(AnimalBreedRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AnimalBreed requireAssignable(UUID breedId) {
        AnimalBreed breed = repository.findById(breedId).orElseThrow(ResourceNotFoundException::new);
        if (!breed.isActive()) {
            throw new ConflictException(ErrorCodes.BREED_INACTIVE, "error.breed.inactive");
        }
        return breed;
    }
}
