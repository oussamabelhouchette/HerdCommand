package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.platform.farm.CreatePlatformFarmRequest;
import com.herdcommand.api.api.platform.farm.PlatformFarmCreatedResponse;
import com.herdcommand.api.domain.feature.FeatureCatalogService;
import com.herdcommand.api.domain.identity.IdentityProvisioningService;
import com.herdcommand.api.domain.identity.OwnerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.dao.DataIntegrityViolationException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FarmOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(FarmOnboardingService.class);

    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    private final FarmOnboardingIdempotencyService idempotencyService;
    private final FarmOnboardingPersistence persistence;
    private final FarmOnboardingCompensationService compensationService;
    private final IdentityProvisioningService identityProvisioningService;
    private final FeatureCatalogService featureCatalogService;

    public FarmOnboardingService(
            FarmOnboardingIdempotencyService idempotencyService,
            FarmOnboardingPersistence persistence,
            FarmOnboardingCompensationService compensationService,
            IdentityProvisioningService identityProvisioningService,
            FeatureCatalogService featureCatalogService) {
        this.idempotencyService = idempotencyService;
        this.persistence = persistence;
        this.compensationService = compensationService;
        this.identityProvisioningService = identityProvisioningService;
        this.featureCatalogService = featureCatalogService;
    }

    public PlatformFarmCreatedResponse create(UUID idempotencyKey, CreatePlatformFarmRequest request) {
        ValidatedFarmOnboarding validated = validate(request);
        String key = idempotencyKey.toString();
        String fingerprint = FarmOnboardingFingerprint.sha256(request);
        Object lock = keyLocks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            try {
                return createClaimed(key, fingerprint, validated);
            } finally {
                keyLocks.remove(key, lock);
            }
        }
    }

    private PlatformFarmCreatedResponse createClaimed(
            String key, String fingerprint, ValidatedFarmOnboarding validated) {
        OnboardingClaim claim;
        try {
            claim = idempotencyService.claim(key, fingerprint);
        } catch (RuntimeException ex) {
            if (!isIdempotencyRace(ex)) {
                throw ex;
            }
            return idempotencyService.awaitReplay(key, fingerprint);
        }
        if (claim instanceof OnboardingClaim.Replay replay) {
            return replay.response();
        }
        if (claim instanceof OnboardingClaim.InProgress) {
            return idempotencyService.awaitReplay(key, fingerprint);
        }
        UUID claimId = ((OnboardingClaim.Proceed) claim).requestId();

        OwnerResolution owner = null;
        try {
            owner = identityProvisioningService.resolveOwner(
                    validated.ownerEmail(), validated.ownerDisplayName());
            if (owner.identityCreated()) {
                idempotencyService.rememberCreatedIdentity(claimId, owner.keycloakUserId());
            }
            return persistence.persistAndComplete(claimId, validated, owner);
        } catch (RuntimeException ex) {
            idempotencyService.markFailed(claimId);
            compensateCreatedIdentity(key, owner);
            throw ex;
        }
    }

    private static boolean isIdempotencyRace(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            String message = String.valueOf(current.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("null value") || message.contains("not-null") || message.contains("23502")) {
                return false;
            }
            if (message.contains("ux_farm_onboarding_key")
                    || (current instanceof DataIntegrityViolationException
                            && (message.contains("duplicate") || message.contains("unique")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    ValidatedFarmOnboarding validate(CreatePlatformFarmRequest request) {
        requireInitialSetup(request.initialStatus());
        requireCurrency(request.currencyCode());
        String nameFr = request.nameFr();
        String nameEn = request.nameEn() == null || request.nameEn().isBlank() ? nameFr : request.nameEn();
        CreateFarmCommand farm = new CreateFarmCommand(
                request.nameAr(),
                nameEn,
                nameFr,
                request.governorateCode(),
                request.address(),
                request.timezone(),
                request.defaultLanguage());
        // Reuse the same field rules as persist without writing.
        previewFarmFields(farm);

        FarmPlanCode plan = FarmPlanLimits.requirePlan(request.subscription().planCode());
        ZoneId zone = resolveZone(request.timezone());
        FarmPlanLimits.Applied subscription = FarmPlanLimits.resolve(
                plan,
                request.subscription().maxActiveAnimals(),
                request.subscription().maxTeamMembers(),
                parseTrialDate(request.subscription().trialEndsAt()),
                zone);

        List<String> features = distinctFeatureCodes(request.enabledFeatureCodes());
        features.forEach(featureCatalogService::requireEnableableByCode);

        return new ValidatedFarmOnboarding(
                farm,
                request.owner().email(),
                request.owner().displayName(),
                subscription,
                features);
    }

    private void previewFarmFields(CreateFarmCommand command) {
        if (command.nameAr() == null || command.nameAr().isBlank() || command.nameAr().length() > 150) {
            throw validation("nameAr");
        }
        if (command.nameEn() == null || command.nameEn().isBlank() || command.nameEn().length() > 150) {
            throw validation("nameEn");
        }
        if (command.nameFr() == null || command.nameFr().isBlank() || command.nameFr().length() > 150) {
            throw validation("nameFr");
        }
        try {
            GovernorateCodes.requireKnown(command.governorateCode());
        } catch (IllegalArgumentException ex) {
            throw validation("governorateCode");
        }
        try {
            FarmLanguage.fromCode(command.defaultLanguage());
        } catch (IllegalArgumentException ex) {
            throw validation("defaultLanguage");
        }
        resolveZone(command.timezone());
        if (command.address() != null && command.address().length() > 500) {
            throw validation("address");
        }
    }

    private static void requireInitialSetup(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String status = raw.trim().toUpperCase(Locale.ROOT);
        if (!FarmStatus.SETUP.name().equals(status)) {
            throw new BadRequestException(
                    ErrorCodes.INVALID_FARM_STATUS_TRANSITION,
                    "error.farm.statusTransition",
                    List.of(new ApiError.FieldError("initialStatus", "invalid")));
        }
    }

    private static void requireCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        if (!Farm.CURRENCY_TND.equalsIgnoreCase(raw.trim())) {
            throw validation("currencyCode");
        }
    }

    private static ZoneId resolveZone(String raw) {
        String timezone = raw == null || raw.isBlank() ? Farm.DEFAULT_TIMEZONE : raw.trim();
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw validation("timezone");
        }
    }

    private static LocalDate parseTrialDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                    ErrorCodes.PLAN_LIMIT_INVALID,
                    "error.plan.limitInvalid",
                    List.of(new ApiError.FieldError("subscription.trialEndsAt", "invalid")));
        }
    }

    private static List<String> distinctFeatureCodes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String code : raw) {
            if (code == null || code.isBlank()) {
                throw new ApiException(ErrorCodes.FEATURE_NOT_FOUND, HttpStatus.NOT_FOUND, "error.feature.notFound");
            }
            codes.add(code.trim().toUpperCase(Locale.ROOT));
        }
        return new ArrayList<>(codes);
    }

    private void compensateCreatedIdentity(String idempotencyKey, OwnerResolution owner) {
        if (owner == null || !owner.identityCreated()) {
            return;
        }
        try {
            identityProvisioningService.compensateCreatedIdentity(owner.keycloakUserId());
        } catch (RuntimeException ex) {
            log.warn("Keeping recoverable compensation record after Keycloak delete failed");
            compensationService.record(idempotencyKey, owner.keycloakUserId(), "db-failure-after-invite");
        }
    }

    private static ApiException validation(String field) {
        return new ApiException(
                ErrorCodes.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST,
                "error.validation",
                List.of(new ApiError.FieldError(field, "invalid")));
    }
}
