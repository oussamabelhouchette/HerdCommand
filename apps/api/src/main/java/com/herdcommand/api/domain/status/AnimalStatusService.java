package com.herdcommand.api.domain.status;

import com.herdcommand.api.api.admin.status.StatusActiveRequest;
import com.herdcommand.api.api.admin.status.StatusResponse;
import com.herdcommand.api.api.admin.status.UpdateStatusRequest;
import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import com.herdcommand.api.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimalStatusService {

    private final AnimalStatusDefinitionRepository repository;
    private final MessageSource messageSource;

    public AnimalStatusService(AnimalStatusDefinitionRepository repository, MessageSource messageSource) {
        this.repository = repository;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public PageResponse<StatusResponse> search(String search, Boolean active, Pageable pageable) {
        String term = search == null ? null : search.trim();
        Page<AnimalStatusDefinition> page = repository.search(term, active, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(StatusResponse::from).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getSort().toString());
    }

    @Transactional(readOnly = true)
    public StatusResponse get(String code) {
        return StatusResponse.from(requireStatus(code));
    }

    @Cacheable(cacheNames = CacheConfig.ANIMAL_STATUS_ACTIVE, key = "'all'")
    @Transactional(readOnly = true)
    public List<StatusResponse> listActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(StatusResponse::from)
                .toList();
    }

    @CacheEvict(cacheNames = CacheConfig.ANIMAL_STATUS_ACTIVE, allEntries = true)
    @Transactional
    public StatusResponse update(String code, UpdateStatusRequest request) {
        AnimalStatusDefinition status = requireStatus(code);
        ColorToken color = ColorToken.tryParse(request.colorToken());
        if (color == null) {
            throw invalidColor();
        }
        applyActive(status, request.active());
        status.setLabelAr(request.labelAr());
        status.setLabelEn(request.labelEn());
        status.setColorToken(color);
        status.setDisplayOrder(request.displayOrder());
        status.setVisibleInFilter(request.visibleInFilter());
        return StatusResponse.from(saveAndFlush(status));
    }

    @CacheEvict(cacheNames = CacheConfig.ANIMAL_STATUS_ACTIVE, allEntries = true)
    @Transactional
    public StatusResponse updateStatus(String code, StatusActiveRequest request) {
        AnimalStatusDefinition status = requireStatus(code);
        applyActive(status, request.active());
        return StatusResponse.from(saveAndFlush(status));
    }

    private AnimalStatusDefinition saveAndFlush(AnimalStatusDefinition status) {
        AnimalStatusDefinition saved = repository.save(status);
        repository.flush();
        return saved;
    }

    AnimalStatusDefinition requireStatus(String code) {
        if (code == null || code.isBlank()) {
            throw new ResourceNotFoundException();
        }
        return repository
                .findByCodeIgnoreCase(code.trim())
                .orElseThrow(ResourceNotFoundException::new);
    }

    private void applyActive(AnimalStatusDefinition status, boolean active) {
        if (!active && status.isRequiredLifecycleStatus()) {
            throw new ConflictException(ErrorCodes.STATUS_REQUIRED, "error.status.required");
        }
        status.setActive(active);
    }

    private ApiException invalidColor() {
        return new ApiException(
                ErrorCodes.INVALID_COLOR_TOKEN,
                HttpStatus.BAD_REQUEST,
                "error.status.invalidColor",
                List.of(new ApiError.FieldError("colorToken", message("error.status.invalidColor"))));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
