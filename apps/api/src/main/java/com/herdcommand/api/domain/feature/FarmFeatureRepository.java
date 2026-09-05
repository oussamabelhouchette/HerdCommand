package com.herdcommand.api.domain.feature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmFeatureRepository extends JpaRepository<FarmFeature, UUID> {

    Optional<FarmFeature> findByFarmIdAndFeatureId(UUID farmId, UUID featureId);

    Optional<FarmFeature> findByIdAndFarmId(UUID id, UUID farmId);

    List<FarmFeature> findByFarmId(UUID farmId);

    @Query("""
            SELECT ff.farmId AS farmId, fc.code AS code
            FROM FarmFeature ff, FeatureCatalog fc
            WHERE ff.featureId = fc.id
              AND ff.enabled = true
              AND ff.farmId IN :farmIds
            ORDER BY fc.displayOrder ASC, fc.code ASC
            """)
    List<FarmFeatureCodeView> findEnabledFeatureCodes(@Param("farmIds") Collection<UUID> farmIds);
}
