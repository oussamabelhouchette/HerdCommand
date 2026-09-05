package com.herdcommand.api.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmMembershipRepository extends JpaRepository<FarmMembership, UUID> {

    Optional<FarmMembership> findByIdAndFarmId(UUID id, UUID farmId);

    Optional<FarmMembership> findByFarmIdAndKeycloakUserId(UUID farmId, String keycloakUserId);

    List<FarmMembership> findByFarmId(UUID farmId);

    long countByFarmIdAndRoleCodeAndStatus(
            UUID farmId, FarmMembershipRole roleCode, FarmMembershipStatus status);

    boolean existsByFarmIdAndKeycloakUserId(UUID farmId, String keycloakUserId);
}
