package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class FarmTenantService {

    private final FarmRepository farmRepository;
    private final FarmMembershipRepository membershipRepository;
    private final FarmSubscriptionRepository subscriptionRepository;
    private final FarmCodeGenerator farmCodeGenerator;

    public FarmTenantService(
            FarmRepository farmRepository,
            FarmMembershipRepository membershipRepository,
            FarmSubscriptionRepository subscriptionRepository,
            FarmCodeGenerator farmCodeGenerator) {
        this.farmRepository = farmRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.farmCodeGenerator = farmCodeGenerator;
    }

    @Transactional
    public FarmTenantSnapshot createFarm(CreateFarmCommand command) {
        Farm farm = persistDraftFarm(command);
        persistSubscription(
                farm.getId(),
                FarmPlanCode.TRIAL,
                FarmPlanLimits.TRIAL_MAX_ANIMALS,
                FarmPlanLimits.TRIAL_MAX_TEAM,
                Instant.now().plus(FarmPlanLimits.TRIAL_DAYS, ChronoUnit.DAYS));
        return FarmTenantSnapshot.from(farm);
    }

    @Transactional
    public Farm persistDraftFarm(CreateFarmCommand command) {
        String nameAr = requireName(command.nameAr(), "nameAr");
        String nameEn = requireName(command.nameEn(), "nameEn");
        String nameFr = requireName(command.nameFr(), "nameFr");
        String governorate;
        try {
            governorate = GovernorateCodes.requireKnown(command.governorateCode());
        } catch (IllegalArgumentException ex) {
            throw validation("governorateCode");
        }
        FarmLanguage language;
        try {
            language = FarmLanguage.fromCode(command.defaultLanguage());
        } catch (IllegalArgumentException ex) {
            throw validation("defaultLanguage");
        }
        String timezone = resolveTimezone(command.timezone());
        String address = blankToNull(command.address());
        if (address != null && address.length() > 500) {
            throw validation("address");
        }

        String code = farmCodeGenerator.nextCode();
        if (farmRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException(ErrorCodes.FARM_CODE_ALREADY_EXISTS, "error.farm.codeExists");
        }

        return farmRepository.saveAndFlush(new Farm(
                code, nameAr, nameEn, nameFr, governorate, address, timezone, language));
    }

    @Transactional
    public FarmSubscription persistSubscription(
            UUID farmId,
            FarmPlanCode planCode,
            int maxActiveAnimals,
            int maxTeamMembers,
            Instant trialEndsAt) {
        return subscriptionRepository.saveAndFlush(new FarmSubscription(
                farmId, planCode, maxActiveAnimals, maxTeamMembers, trialEndsAt));
    }

    @Transactional
    public FarmTenantSnapshot activate(UUID farmId) {
        Farm farm = requireFarm(farmId);
        if (countActiveOwners(farmId) < 1) {
            throw new ConflictException(ErrorCodes.FARM_OWNER_REQUIRED, "error.farm.ownerRequired");
        }
        farm.applyStatus(FarmStatus.ACTIVE);
        return FarmTenantSnapshot.from(farmRepository.saveAndFlush(farm));
    }

    @Transactional
    public FarmMembership assignOwner(AssignFarmOwnerCommand command) {
        return assignOwner(command, FarmMembershipStatus.ACTIVE);
    }

    @Transactional
    public FarmMembership assignOwner(AssignFarmOwnerCommand command, FarmMembershipStatus status) {
        Farm farm = requireFarm(command.farmId());
        String userId = requireText(command.keycloakUserId(), "keycloakUserId", 64);
        String email = FarmMembership.normalizeEmail(requireText(command.invitedEmail(), "invitedEmail", 320));
        if (membershipRepository.existsByFarmIdAndKeycloakUserId(farm.getId(), userId)) {
            throw new ConflictException(ErrorCodes.OWNER_ALREADY_ASSIGNED, "error.farm.ownerAssigned");
        }
        Instant now = Instant.now();
        FarmMembership membership = new FarmMembership(
                farm.getId(),
                userId,
                email,
                now,
                currentAuditor(),
                status == null ? FarmMembershipStatus.ACTIVE : status,
                command.displayName());
        return membershipRepository.saveAndFlush(membership);
    }

    @Transactional
    public void removeOwner(UUID farmId, UUID membershipId) {
        Farm farm = requireFarm(farmId);
        FarmMembership membership = membershipRepository
                .findByIdAndFarmId(membershipId, farmId)
                .orElseThrow(ResourceNotFoundException::new);
        boolean lastActiveOwner = membership.isActiveOwner() && countActiveOwners(farmId) == 1;
        if (farm.getStatus() == FarmStatus.ACTIVE && lastActiveOwner) {
            throw new ConflictException(ErrorCodes.FARM_OWNER_REQUIRED, "error.farm.ownerRequired");
        }
        membership.applyStatus(FarmMembershipStatus.REMOVED, Instant.now());
        membershipRepository.saveAndFlush(membership);
    }

    @Transactional
    public FarmTenantSnapshot updateIdentity(UpdateFarmIdentityCommand command) {
        Farm farm = requireFarm(command.farmId());
        if (farm.getVersion() == null || farm.getVersion() != command.expectedVersion()) {
            throw new ConflictException(ErrorCodes.FARM_VERSION_CONFLICT, "error.farm.versionConflict");
        }
        farm.setNameAr(requireName(command.nameAr(), "nameAr"));
        farm.setNameEn(requireName(command.nameEn(), "nameEn"));
        farm.setNameFr(requireName(command.nameFr(), "nameFr"));
        try {
            farm.setGovernorateCode(GovernorateCodes.requireKnown(command.governorateCode()));
        } catch (IllegalArgumentException ex) {
            throw validation("governorateCode");
        }
        try {
            farm.setDefaultLanguage(FarmLanguage.fromCode(command.defaultLanguage()));
        } catch (IllegalArgumentException ex) {
            throw validation("defaultLanguage");
        }
        farm.setTimezone(resolveTimezone(command.timezone()));
        String address = blankToNull(command.address());
        if (address != null && address.length() > 500) {
            throw validation("address");
        }
        farm.setAddress(address);
        return FarmTenantSnapshot.from(farmRepository.saveAndFlush(farm));
    }

    @Transactional(readOnly = true)
    public Farm requireFarm(UUID farmId) {
        return farmRepository.findById(farmId).orElseThrow(ResourceNotFoundException::new);
    }

    private long countActiveOwners(UUID farmId) {
        return membershipRepository.countByFarmIdAndRoleCodeAndStatus(
                farmId, FarmMembershipRole.FARM_OWNER, FarmMembershipStatus.ACTIVE);
    }

    private static String requireName(String value, String field) {
        return requireText(value, field, 150);
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw validation(field);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw validation(field);
        }
        return trimmed;
    }

    private static String resolveTimezone(String raw) {
        String timezone = raw == null || raw.isBlank() ? Farm.DEFAULT_TIMEZONE : raw.trim();
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw validation("timezone");
        }
        return timezone;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static ApiException validation(String field) {
        return new ApiException(
                ErrorCodes.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST,
                "error.validation",
                List.of(new ApiError.FieldError(field, "invalid")));
    }

    private static String currentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return jwt.getSubject();
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }
}
