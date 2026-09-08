package com.herdcommand.api.domain.farm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmRepository extends JpaRepository<Farm, UUID> {

    Optional<Farm> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Farm> findAllByOrderByNameArAsc();

    @Query("""
            SELECT f FROM Farm f
            WHERE (:status IS NULL OR f.status = :status)
              AND (
                    :planCode IS NULL
                    OR EXISTS (
                        SELECT 1 FROM FarmSubscription s
                        WHERE s.farmId = f.id AND s.planCode = :planCode
                    )
              )
              AND (
                    :featureCode IS NULL OR :featureCode = ''
                    OR EXISTS (
                        SELECT 1 FROM FarmFeature ff, FeatureCatalog fc
                        WHERE ff.farmId = f.id
                          AND ff.featureId = fc.id
                          AND ff.enabled = true
                          AND LOWER(fc.code) = LOWER(:featureCode)
                    )
              )
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(f.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(f.nameAr) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(f.nameEn) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(f.nameFr) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR EXISTS (
                        SELECT 1 FROM FarmMembership m
                        WHERE m.farmId = f.id
                          AND m.roleCode = :ownerRole
                          AND m.status <> :removedStatus
                          AND (
                                LOWER(m.invitedEmail) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(m.displayName) LIKE LOWER(CONCAT('%', :search, '%'))
                          )
                    )
              )
            """)
    Page<Farm> searchPlatformFarms(
            @Param("search") String search,
            @Param("status") FarmStatus status,
            @Param("planCode") FarmPlanCode planCode,
            @Param("featureCode") String featureCode,
            @Param("ownerRole") FarmMembershipRole ownerRole,
            @Param("removedStatus") FarmMembershipStatus removedStatus,
            Pageable pageable);

    @Query("SELECT f.status, COUNT(f) FROM Farm f GROUP BY f.status")
    List<Object[]> countGroupedByStatus();
}
