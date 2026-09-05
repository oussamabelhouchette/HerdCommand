package com.herdcommand.api.domain.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureCatalogRepository extends JpaRepository<FeatureCatalog, UUID> {

    Optional<FeatureCatalog> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<FeatureCatalog> findByActiveTrueAndReleaseStatusOrderByDisplayOrderAscCodeAsc(
            FeatureReleaseStatus releaseStatus);

    List<FeatureCatalog> findByActiveTrueAndReleaseStatusInOrderByDisplayOrderAscCodeAsc(
            List<FeatureReleaseStatus> releaseStatuses);
}
