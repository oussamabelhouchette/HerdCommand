package com.herdcommand.api.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FarmSubscriptionRepository extends JpaRepository<FarmSubscription, UUID> {

    Optional<FarmSubscription> findByFarmId(UUID farmId);

    Optional<FarmSubscription> findByIdAndFarmId(UUID id, UUID farmId);
}
