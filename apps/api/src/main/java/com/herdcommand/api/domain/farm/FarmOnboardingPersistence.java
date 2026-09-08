package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.platform.farm.PlatformFarmCreatedResponse;
import com.herdcommand.api.domain.feature.FarmFeatureService;
import com.herdcommand.api.domain.identity.OwnerResolution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FarmOnboardingPersistence {

    private final FarmTenantService farmTenantService;
    private final FarmFeatureService farmFeatureService;
    private final FarmOnboardingRequestRepository requestRepository;
    private final FarmOnboardingIdempotencyService idempotencyService;

    public FarmOnboardingPersistence(
            FarmTenantService farmTenantService,
            FarmFeatureService farmFeatureService,
            FarmOnboardingRequestRepository requestRepository,
            FarmOnboardingIdempotencyService idempotencyService) {
        this.farmTenantService = farmTenantService;
        this.farmFeatureService = farmFeatureService;
        this.requestRepository = requestRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PlatformFarmCreatedResponse persistAndComplete(
            UUID claimId, ValidatedFarmOnboarding validated, OwnerResolution owner) {
        Farm farm = farmTenantService.persistDraftFarm(validated.farm());
        farmTenantService.persistSubscription(
                farm.getId(),
                validated.subscription().planCode(),
                validated.subscription().maxActiveAnimals(),
                validated.subscription().maxTeamMembers(),
                validated.subscription().trialEndsAt());
        farmTenantService.assignOwner(
                new AssignFarmOwnerCommand(
                        farm.getId(), owner.keycloakUserId(), owner.email(), owner.displayName()),
                owner.membershipStatus());
        List<String> enabled = farmFeatureService.enableByCodes(farm.getId(), validated.enabledFeatureCodes());

        PlatformFarmCreatedResponse response = new PlatformFarmCreatedResponse(
                farm.getId(),
                farm.getCode(),
                farm.getStatus().name(),
                owner.membershipStatus().name(),
                owner.invitationEmailSent(),
                enabled);

        FarmOnboardingRequest claim = requestRepository.findById(claimId).orElseThrow();
        claim.complete(farm.getId(), idempotencyService.writeResponse(response));
        requestRepository.saveAndFlush(claim);
        return response;
    }
}
