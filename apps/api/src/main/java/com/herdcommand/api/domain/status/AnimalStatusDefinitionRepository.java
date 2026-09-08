package com.herdcommand.api.domain.status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalStatusDefinitionRepository extends JpaRepository<AnimalStatusDefinition, String> {

    Optional<AnimalStatusDefinition> findByCodeIgnoreCase(String code);

    List<AnimalStatusDefinition> findByActiveTrueOrderByDisplayOrderAsc();

    List<AnimalStatusDefinition> findByActiveTrueAndVisibleInFilterTrueOrderByDisplayOrderAsc();

    @Query("""
            SELECT s FROM AnimalStatusDefinition s
            WHERE (:active IS NULL OR s.active = :active)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(s.labelAr) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(s.labelEn) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AnimalStatusDefinition> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}
