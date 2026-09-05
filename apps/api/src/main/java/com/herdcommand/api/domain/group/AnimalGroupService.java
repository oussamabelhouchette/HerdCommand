package com.herdcommand.api.domain.group;

import com.herdcommand.api.api.common.PageResponse;
import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import com.herdcommand.api.api.farm.group.CreateGroupRequest;
import com.herdcommand.api.api.farm.group.GroupResponse;
import com.herdcommand.api.api.farm.group.GroupStatusRequest;
import com.herdcommand.api.api.farm.group.UpdateGroupRequest;
import com.herdcommand.api.domain.farm.FarmService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnimalGroupService {

    private final AnimalGroupRepository groupRepository;
    private final FarmService farmService;
    private final MessageSource messageSource;

    public AnimalGroupService(
            AnimalGroupRepository groupRepository, FarmService farmService, MessageSource messageSource) {
        this.groupRepository = groupRepository;
        this.farmService = farmService;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public PageResponse<GroupResponse> search(
            UUID farmId, String search, GroupTypeCode groupTypeCode, Boolean active, Pageable pageable) {
        farmService.requireFarm(farmId);
        String term = search == null ? null : search.trim();
        Page<AnimalGroup> page = groupRepository.search(farmId, term, groupTypeCode, active, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(GroupResponse::from).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getSort().toString());
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID farmId, UUID groupId) {
        return GroupResponse.from(requireGroup(farmId, groupId));
    }

    @Transactional
    public GroupResponse create(UUID farmId, CreateGroupRequest request) {
        farmService.requireFarm(farmId);
        if (groupRepository.existsByFarmIdAndCodeIgnoreCase(farmId, request.code())) {
            throw duplicateCode();
        }
        AnimalGroup group = new AnimalGroup(
                farmId,
                request.code(),
                request.nameAr(),
                request.nameEn(),
                request.groupTypeCode(),
                request.description(),
                request.capacity());
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse update(UUID farmId, UUID groupId, UpdateGroupRequest request) {
        AnimalGroup group = requireGroup(farmId, groupId);
        if (request.code() != null && !request.code().isBlank() && !request.code().equals(group.getCode())) {
            throw new ApiException(
                    ErrorCodes.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "error.validation",
                    List.of(new ApiError.FieldError("code", message("error.group.codeImmutable"))));
        }
        group.setNameAr(request.nameAr());
        group.setNameEn(request.nameEn());
        group.setGroupTypeCode(request.groupTypeCode());
        group.setDescription(request.description());
        group.setCapacity(request.capacity());
        return GroupResponse.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResponse updateStatus(UUID farmId, UUID groupId, GroupStatusRequest request) {
        AnimalGroup group = requireGroup(farmId, groupId);
        group.setActive(request.active());
        return GroupResponse.from(groupRepository.save(group));
    }

    private AnimalGroup requireGroup(UUID farmId, UUID groupId) {
        farmService.requireFarm(farmId);
        return groupRepository.findByIdAndFarmId(groupId, farmId).orElseThrow(ResourceNotFoundException::new);
    }

    private ConflictException duplicateCode() {
        return new ConflictException(
                ErrorCodes.GROUP_CODE_ALREADY_EXISTS,
                "error.group.codeExists",
                List.of(new ApiError.FieldError("code", message("validation.codeUnique"))));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
