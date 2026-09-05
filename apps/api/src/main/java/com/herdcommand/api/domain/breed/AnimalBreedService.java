package com.herdcommand.api.domain.breed;

import com.herdcommand.api.api.admin.breed.BreedResponse;
import com.herdcommand.api.api.admin.breed.BreedStatusRequest;
import com.herdcommand.api.api.admin.breed.CreateBreedRequest;
import com.herdcommand.api.api.admin.breed.UpdateBreedRequest;
import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnimalBreedService {

    private final AnimalBreedRepository repository;
    private final org.springframework.context.MessageSource messageSource;

    public AnimalBreedService(
            AnimalBreedRepository repository,
            org.springframework.context.MessageSource messageSource) {
        this.repository = repository;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public PageResponse<BreedResponse> search(String search, SpeciesCode speciesCode, Boolean active, Pageable pageable) {
        String term = search == null ? null : search.trim();
        Page<AnimalBreed> page = repository.search(term, speciesCode, active, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(BreedResponse::from).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getSort().toString());
    }

    @Transactional(readOnly = true)
    public BreedResponse get(UUID breedId) {
        return BreedResponse.from(requireBreed(breedId));
    }

    @Transactional
    public BreedResponse create(CreateBreedRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw duplicateCode();
        }
        AnimalBreed breed = new AnimalBreed(
                request.code(),
                request.nameAr(),
                request.nameEn(),
                request.speciesCode(),
                request.displayOrder());
        return BreedResponse.from(repository.save(breed));
    }

    @Transactional
    public BreedResponse update(UUID breedId, UpdateBreedRequest request) {
        AnimalBreed breed = requireBreed(breedId);
        if (request.code() != null && !request.code().isBlank() && !request.code().equals(breed.getCode())) {
            throw new ApiException(
                    ErrorCodes.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "error.validation",
                    List.of(new ApiError.FieldError("code", message("error.breed.codeImmutable"))));
        }
        breed.setNameAr(request.nameAr());
        breed.setNameEn(request.nameEn());
        breed.setSpeciesCode(request.speciesCode());
        breed.setDisplayOrder(request.displayOrder());
        return BreedResponse.from(repository.save(breed));
    }

    @Transactional
    public BreedResponse updateStatus(UUID breedId, BreedStatusRequest request) {
        AnimalBreed breed = requireBreed(breedId);
        breed.setActive(request.active());
        return BreedResponse.from(repository.save(breed));
    }

    private AnimalBreed requireBreed(UUID breedId) {
        return repository.findById(breedId).orElseThrow(ResourceNotFoundException::new);
    }

    private ConflictException duplicateCode() {
        return new ConflictException(
                ErrorCodes.BREED_CODE_ALREADY_EXISTS,
                "error.breed.codeExists",
                List.of(new ApiError.FieldError("code", message("validation.codeUnique"))));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, key, org.springframework.context.i18n.LocaleContextHolder.getLocale());
    }
}
