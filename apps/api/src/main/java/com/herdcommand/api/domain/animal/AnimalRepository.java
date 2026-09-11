package com.herdcommand.api.domain.animal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    Optional<Animal> findByIdAndFarmIdAndArchivedAtIsNull(UUID id, UUID farmId);

    boolean existsByFarmIdAndIdentificationNumberIgnoreCase(UUID farmId, String identificationNumber);

    boolean existsByFarmIdAndIdentificationNumberIgnoreCaseAndIdNot(
            UUID farmId, String identificationNumber, UUID id);

    long countByFarmIdAndGroupIdAndArchivedAtIsNull(UUID farmId, UUID groupId);

    @Query("""
            SELECT a FROM Animal a
            WHERE a.farmId = :farmId
              AND a.archivedAt IS NULL
              AND (:breedId IS NULL OR a.breedId = :breedId)
              AND (:statusCode IS NULL OR a.statusCode = :statusCode)
              AND (:groupId IS NULL OR a.groupId = :groupId)
              AND (:genderCode IS NULL OR a.genderCode = :genderCode)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(a.identificationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(a.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Animal> search(
            @Param("farmId") UUID farmId,
            @Param("search") String search,
            @Param("breedId") UUID breedId,
            @Param("statusCode") String statusCode,
            @Param("groupId") UUID groupId,
            @Param("genderCode") GenderCode genderCode,
            Pageable pageable);
}
