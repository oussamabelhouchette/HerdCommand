package com.herdcommand.api.domain.farm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmOnboardingCompensationService {

    private final FarmOnboardingCompensationRepository compensationRepository;

    public FarmOnboardingCompensationService(FarmOnboardingCompensationRepository compensationRepository) {
        this.compensationRepository = compensationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String idempotencyKey, String keycloakUserId, String reason) {
        compensationRepository.saveAndFlush(new FarmOnboardingCompensation(
                idempotencyKey, keycloakUserId, reason));
    }
}
