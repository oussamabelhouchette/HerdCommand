package com.herdcommand.api.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FarmOnboardingRequestRepository extends JpaRepository<FarmOnboardingRequest, UUID> {

    Optional<FarmOnboardingRequest> findByIdempotencyKey(String idempotencyKey);
}
