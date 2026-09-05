package com.herdcommand.api.domain.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmFeatureRepository extends JpaRepository<FarmFeature, UUID> {

    Optional<FarmFeature> findByFarmIdAndFeatureId(UUID farmId, UUID featureId);

    Optional<FarmFeature> findByIdAndFarmId(UUID id, UUID farmId);

    List<FarmFeature> findByFarmId(UUID farmId);
}
