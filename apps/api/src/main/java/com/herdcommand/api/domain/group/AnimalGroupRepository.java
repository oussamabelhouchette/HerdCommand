package com.herdcommand.api.domain.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface AnimalGroupRepository extends JpaRepository<AnimalGroup, UUID> {

    boolean existsByFarmIdAndCodeIgnoreCase(UUID farmId, String code);

    Optional<AnimalGroup> findByIdAndFarmId(UUID id, UUID farmId);

    List<AnimalGroup> findByFarmIdAndActiveTrueOrderByNameArAsc(UUID farmId);

    @Query("""
            SELECT g FROM AnimalGroup g
            WHERE g.farmId = :farmId
              AND (:groupTypeCode IS NULL OR g.groupTypeCode = :groupTypeCode)
              AND (:active IS NULL OR g.active = :active)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(g.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(g.nameAr) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(g.nameEn) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AnimalGroup> search(
            @Param("farmId") UUID farmId,
            @Param("search") String search,
            @Param("groupTypeCode") GroupTypeCode groupTypeCode,
            @Param("active") Boolean active,
            Pageable pageable);
}
