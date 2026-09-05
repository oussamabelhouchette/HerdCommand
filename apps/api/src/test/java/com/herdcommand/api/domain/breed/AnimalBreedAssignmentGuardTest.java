package com.herdcommand.api.domain.breed;

import com.herdcommand.api.api.error.ConflictException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.api.error.ResourceNotFoundException;
import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class AnimalBreedAssignmentGuardTest {

    @Autowired
    private AnimalBreedRepository repository;

    @Autowired
    private AnimalBreedAssignmentGuard guard;

    @BeforeEach
    void authenticate() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("assigner-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeBreedCanBeAssigned() {
        AnimalBreed breed = repository.save(new AnimalBreed("ASSIGN_" + id(), "قابل", "Assignable", SpeciesCode.SHEEP, 0));

        AnimalBreed assignable = guard.requireAssignable(breed.getId());

        assertThat(assignable.getId()).isEqualTo(breed.getId());
        assertThat(assignable.isActive()).isTrue();
    }

    @Test
    void inactiveBreedCannotBeNewlyAssigned() {
        AnimalBreed breed = repository.save(new AnimalBreed("INACTIVE_" + id(), "غير نشط", "Inactive", SpeciesCode.SHEEP, 0));
        breed.setActive(false);
        repository.save(breed);

        assertThatThrownBy(() -> guard.requireAssignable(breed.getId()))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).getCode())
                .isEqualTo(ErrorCodes.BREED_INACTIVE);
    }

    @Test
    void missingBreedCannotBeAssigned() {
        assertThatThrownBy(() -> guard.requireAssignable(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static String id() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
