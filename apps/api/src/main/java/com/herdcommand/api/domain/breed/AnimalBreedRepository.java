package com.herdcommand.api.domain.breed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnimalBreedRepository extends JpaRepository<AnimalBreed, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<AnimalBreed> findByActiveTrueOrderByDisplayOrderAscCodeAsc();

    @Query("""
            SELECT b FROM AnimalBreed b
            WHERE (:speciesCode IS NULL OR b.speciesCode = :speciesCode)
              AND (:active IS NULL OR b.active = :active)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(b.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(b.nameAr) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(b.nameEn) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AnimalBreed> search(
            @Param("search") String search,
            @Param("speciesCode") SpeciesCode speciesCode,
            @Param("active") Boolean active,
            Pageable pageable);
}
