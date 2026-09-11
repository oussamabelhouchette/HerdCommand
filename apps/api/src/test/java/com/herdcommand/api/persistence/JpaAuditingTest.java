package com.herdcommand.api.persistence;

import com.herdcommand.api.config.JpaAuditingConfig;
import com.herdcommand.api.domain.audit.AuditedEntity;
import com.herdcommand.auditprobe.JpaAuditProbe;
import com.herdcommand.auditprobe.JpaAuditProbeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@EntityScan(basePackageClasses = {AuditedEntity.class, JpaAuditProbe.class})
@EnableJpaRepositories(basePackageClasses = JpaAuditProbeRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JpaAuditingTest {

    @Autowired
    private JpaAuditProbeRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void authenticate() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("auditor-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistFillsCreatedAndUpdatedAuditFields() {
        JpaAuditProbe saved = repository.saveAndFlush(new JpaAuditProbe("ewe-group"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("auditor-1");
        assertThat(saved.getUpdatedBy()).isEqualTo("auditor-1");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Instant originalUpdatedAt = saved.getUpdatedAt();
        saved.setName("ram-group");
        JpaAuditProbe updated = repository.saveAndFlush(saved);
        entityManager.refresh(updated);

        assertThat(updated.getCreatedBy()).isEqualTo("auditor-1");
        assertThat(updated.getUpdatedBy()).isEqualTo("auditor-1");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void persistUsesPreferredUsernameWhenJwtHasNoSubject() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "adminfarm")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        JpaAuditProbe saved = repository.saveAndFlush(new JpaAuditProbe("no-sub"));

        assertThat(saved.getCreatedBy()).isEqualTo("adminfarm");
        assertThat(saved.getUpdatedBy()).isEqualTo("adminfarm");
    }
}
