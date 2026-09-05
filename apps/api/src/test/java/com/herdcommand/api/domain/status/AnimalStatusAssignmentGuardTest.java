package com.herdcommand.api.domain.status;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class AnimalStatusAssignmentGuardTest {

    @Autowired
    private AnimalStatusDefinitionRepository repository;

    @Autowired
    private AnimalStatusAssignmentGuard guard;

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
    void activeStatusCanBeAssigned() {
        AnimalStatusDefinition status = guard.requireAssignable("ISOLATED");

        assertThat(status.getCode()).isEqualTo("ISOLATED");
        assertThat(status.isActive()).isTrue();
    }

    @Test
    void inactiveStatusCannotBeNewlyAssigned() {
        AnimalStatusDefinition sick = repository.findByCodeIgnoreCase("SICK").orElseThrow();
        sick.setActive(false);
        repository.save(sick);
        try {
            assertThatThrownBy(() -> guard.requireAssignable("SICK"))
                    .isInstanceOf(ConflictException.class)
                    .extracting(ex -> ((ConflictException) ex).getCode())
                    .isEqualTo(ErrorCodes.STATUS_INACTIVE);
        } finally {
            sick.setActive(true);
            repository.save(sick);
        }
    }

    @Test
    void missingStatusCannotBeAssigned() {
        assertThatThrownBy(() -> guard.requireAssignable("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
