package com.herdcommand.api.domain.animal;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import com.herdcommand.api.api.farm.animal.AnimalLookupResponse;
import com.herdcommand.api.api.farm.animal.AnimalPageResponse;
import com.herdcommand.api.api.farm.animal.AnimalResponse;
import com.herdcommand.api.api.farm.animal.CreateAnimalRequest;
import com.herdcommand.api.api.farm.animal.UpdateAnimalRequest;
import com.herdcommand.api.domain.breed.AnimalBreed;
import com.herdcommand.api.domain.breed.AnimalBreedAssignmentGuard;
import com.herdcommand.api.domain.breed.AnimalBreedRepository;
import com.herdcommand.api.domain.farm.FarmAccessService;
import com.herdcommand.api.domain.feature.FeatureLocales;
import com.herdcommand.api.domain.group.AnimalGroup;
import com.herdcommand.api.domain.group.AnimalGroupRepository;
import com.herdcommand.api.domain.status.AnimalStatusAssignmentGuard;
import com.herdcommand.api.domain.status.AnimalStatusDefinition;
import com.herdcommand.api.domain.status.AnimalStatusDefinitionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnimalService {

    public static final int PAGE_SIZE = 20;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final AnimalRepository animalRepository;
    private final AnimalBreedRepository breedRepository;
    private final AnimalStatusDefinitionRepository statusRepository;
    private final AnimalGroupRepository groupRepository;
    private final AnimalBreedAssignmentGuard breedGuard;
    private final AnimalStatusAssignmentGuard statusGuard;
    private final FarmAccessService farmAccessService;

    public AnimalService(
            AnimalRepository animalRepository,
            AnimalBreedRepository breedRepository,
            AnimalStatusDefinitionRepository statusRepository,
            AnimalGroupRepository groupRepository,
            AnimalBreedAssignmentGuard breedGuard,
            AnimalStatusAssignmentGuard statusGuard,
            FarmAccessService farmAccessService) {
        this.animalRepository = animalRepository;
        this.breedRepository = breedRepository;
        this.statusRepository = statusRepository;
        this.groupRepository = groupRepository;
        this.breedGuard = breedGuard;
        this.statusGuard = statusGuard;
        this.farmAccessService = farmAccessService;
    }

    @Transactional(readOnly = true)
    public AnimalLookupResponse lookups(UUID farmId, String acceptLanguage) {
        farmAccessService.requireOwnerAccess(farmId);
        List<AnimalLookupResponse.LookupGender> genders = List.of(
                new AnimalLookupResponse.LookupGender(GenderCode.FEMALE.name(), "نعجة", "Ewe"),
                new AnimalLookupResponse.LookupGender(GenderCode.MALE.name(), "كبش", "Ram"));
        return new AnimalLookupResponse(
                breedRepository.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
                        .map(breed -> new AnimalLookupResponse.LookupBreed(
                                breed.getId(), breed.getCode(), breed.getNameAr(), breed.getNameEn(), breed.isActive()))
                        .toList(),
                statusRepository.findByActiveTrueAndVisibleInFilterTrueOrderByDisplayOrderAsc().stream()
                        .map(status -> new AnimalLookupResponse.LookupStatus(
                                status.getCode(),
                                status.getLabelAr(),
                                status.getLabelEn(),
                                status.getColorToken().token(),
                                status.isActive()))
                        .toList(),
                groupRepository.findByFarmIdAndActiveTrueOrderByNameArAsc(farmId).stream()
                        .map(group -> new AnimalLookupResponse.LookupGroup(
                                group.getId(), group.getNameAr(), group.getNameEn(), group.isActive()))
                        .toList(),
                genders);
    }

    @Transactional(readOnly = true)
    public AnimalPageResponse search(
            UUID farmId,
            String search,
            UUID breedId,
            String statusCode,
            UUID groupId,
            GenderCode genderCode,
            int page,
            String acceptLanguage) {
        farmAccessService.requireOwnerAccess(farmId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, DEFAULT_SORT);
        Page<Animal> result = animalRepository.search(
                farmId,
                blankToNull(search),
                breedId,
                blankToNull(statusCode),
                groupId,
                genderCode,
                pageable);
        Lookups lookups = loadLookups(result.getContent());
        String language = FeatureLocales.fromAcceptLanguage(acceptLanguage);
        int totalPages = result.getTotalPages();
        return new AnimalPageResponse(
                result.getContent().stream().map(animal -> toResponse(animal, lookups, language)).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                totalPages,
                result.isFirst(),
                result.isLast() || result.getTotalElements() == 0,
                "createdAt,desc");
    }

    @Transactional(readOnly = true)
    public AnimalResponse get(UUID farmId, UUID animalId, String acceptLanguage) {
        farmAccessService.requireOwnerAccess(farmId);
        Animal animal = requireActive(farmId, animalId);
        return toResponse(animal, loadLookups(List.of(animal)), FeatureLocales.fromAcceptLanguage(acceptLanguage));
    }

    @Transactional
    public AnimalResponse create(UUID farmId, CreateAnimalRequest request, String acceptLanguage) {
        farmAccessService.requireOwnerAccess(farmId);
        if (animalRepository.existsByFarmIdAndIdentificationNumberIgnoreCase(farmId, request.identificationNumber())) {
            throw duplicateIdentification();
        }
        AnimalBreed breed = breedGuard.requireAssignable(request.breedId());
        AnimalStatusDefinition status = statusGuard.requireAssignable(request.statusCode());
        UUID groupId = requireGroupInFarm(farmId, request.groupId());
        Animal saved = animalRepository.save(new Animal(
                farmId,
                request.identificationNumber(),
                request.name(),
                breed.getId(),
                status.getCode(),
                request.genderCode(),
                request.dateOfBirth(),
                groupId));
        return toResponse(saved, loadLookups(List.of(saved)), FeatureLocales.fromAcceptLanguage(acceptLanguage));
    }

    @Transactional
    public AnimalResponse update(UUID farmId, UUID animalId, UpdateAnimalRequest request, String acceptLanguage) {
        farmAccessService.requireOwnerAccess(farmId);
        Animal animal = requireActive(farmId, animalId);
        if (animalRepository.existsByFarmIdAndIdentificationNumberIgnoreCaseAndIdNot(
                farmId, request.identificationNumber(), animalId)) {
            throw duplicateIdentification();
        }
        AnimalBreed breed = breedGuard.requireAssignable(request.breedId());
        AnimalStatusDefinition status = statusGuard.requireAssignable(request.statusCode());
        UUID groupId = requireGroupInFarm(farmId, request.groupId());
        animal.setIdentificationNumber(request.identificationNumber());
        animal.setName(request.name());
        animal.setBreedId(breed.getId());
        animal.setStatusCode(status.getCode());
        animal.setGenderCode(request.genderCode());
        animal.setDateOfBirth(request.dateOfBirth());
        animal.setGroupId(groupId);
        return toResponse(animal, loadLookups(List.of(animal)), FeatureLocales.fromAcceptLanguage(acceptLanguage));
    }

    @Transactional
    public void archive(UUID farmId, UUID animalId) {
        farmAccessService.requireOwnerAccess(farmId);
        Animal animal = requireActive(farmId, animalId);
        animal.archive(Instant.now());
    }

    private Animal requireActive(UUID farmId, UUID animalId) {
        return animalRepository.findByIdAndFarmIdAndArchivedAtIsNull(animalId, farmId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private UUID requireGroupInFarm(UUID farmId, UUID groupId) {
        if (groupId == null) {
            return null;
        }
        AnimalGroup group = groupRepository.findByIdAndFarmId(groupId, farmId).orElse(null);
        if (group == null) {
            throw new BadRequestException(ErrorCodes.GROUP_NOT_IN_FARM, "error.animal.groupNotInFarm");
        }
        return group.getId();
    }

    private static ConflictException duplicateIdentification() {
        return new ConflictException(
                ErrorCodes.ANIMAL_ID_ALREADY_EXISTS,
                "error.animal.idExists",
                List.of(new ApiError.FieldError("identificationNumber", "duplicate")));
    }

    private Lookups loadLookups(List<Animal> animals) {
        if (animals.isEmpty()) {
            return Lookups.empty();
        }
        Set<UUID> breedIds = animals.stream().map(Animal::getBreedId).collect(Collectors.toSet());
        Set<String> statusCodes = animals.stream().map(Animal::getStatusCode).collect(Collectors.toSet());
        Set<UUID> groupIds = animals.stream().map(Animal::getGroupId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, AnimalBreed> breeds = breedRepository.findAllById(breedIds).stream()
                .collect(Collectors.toMap(AnimalBreed::getId, Function.identity()));
        Map<String, AnimalStatusDefinition> statuses = new HashMap<>();
        for (AnimalStatusDefinition status : statusRepository.findAllById(statusCodes)) {
            statuses.put(status.getCode(), status);
        }
        Map<UUID, AnimalGroup> groups = groupIds.isEmpty()
                ? Map.of()
                : groupRepository.findAllById(groupIds).stream()
                        .collect(Collectors.toMap(AnimalGroup::getId, Function.identity()));
        return new Lookups(breeds, statuses, groups);
    }

    private static AnimalResponse toResponse(Animal animal, Lookups lookups, String language) {
        AnimalBreed breed = lookups.breeds.get(animal.getBreedId());
        AnimalStatusDefinition status = lookups.statuses.get(animal.getStatusCode());
        AnimalGroup group = animal.getGroupId() == null ? null : lookups.groups.get(animal.getGroupId());
        return new AnimalResponse(
                animal.getId(),
                animal.getFarmId(),
                animal.getIdentificationNumber(),
                animal.getName(),
                breed == null
                        ? null
                        : new AnimalResponse.AnimalBreedRef(breed.getId(), breed.getCode(), breed.getNameAr(), breed.getNameEn()),
                animal.getGenderCode().name(),
                animal.getDateOfBirth(),
                AnimalAgeFormatter.display(animal.getDateOfBirth(), language),
                group == null ? null : new AnimalResponse.AnimalGroupRef(group.getId(), group.getNameAr(), group.getNameEn()),
                status == null
                        ? null
                        : new AnimalResponse.AnimalStatusRef(
                                status.getCode(), status.getLabelAr(), status.getLabelEn(), status.getColorToken().token()),
                animal.getCreatedAt(),
                animal.getUpdatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Lookups(
            Map<UUID, AnimalBreed> breeds,
            Map<String, AnimalStatusDefinition> statuses,
            Map<UUID, AnimalGroup> groups
    ) {
        static Lookups empty() {
            return new Lookups(Map.of(), Map.of(), Map.of());
        }
    }
}
