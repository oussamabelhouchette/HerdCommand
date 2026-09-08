package com.herdcommand.api.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FarmOnboardingCompensationRepository extends JpaRepository<FarmOnboardingCompensation, UUID> {

    List<FarmOnboardingCompensation> findByIdempotencyKey(String idempotencyKey);

    List<FarmOnboardingCompensation> findByResolvedFalseOrderByCreatedAtAsc();
}
