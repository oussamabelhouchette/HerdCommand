package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.FarmRepository;
import com.herdcommand.api.domain.feature.FeatureCatalog;
import com.herdcommand.api.domain.feature.FeatureCatalogRepository;
import com.herdcommand.api.domain.feature.FeatureReleaseStatus;
import com.herdcommand.api.security.Permission;
import com.herdcommand.api.support.JwtAuth;
import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class FeatureCatalogApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void authenticateForAudit() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("platform-admin-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void animalManagementIsAvailableAndEnableable() throws Exception {
        mockMvc.perform(get("/api/v1/platform/features")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].code").value("ANIMAL_MANAGEMENT"))
                .andExpect(jsonPath("$.items[0].releaseStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[0].enableable").value(true))
                .andExpect(jsonPath("$.items[0].name").value("إدارة الحيوانات"));
    }

    @Test
    void comingSoonFeaturesAreReturnedOnlyWhenRequested() throws Exception {
        mockMvc.perform(get("/api/v1/platform/features")
                        .param("includeComingSoon", "true")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("ANIMAL_MANAGEMENT"))
                .andExpect(jsonPath("$.items[0].name").value("Gestion des animaux"))
                .andExpect(jsonPath("$.items[1].code").value("HEALTH"))
                .andExpect(jsonPath("$.items[1].releaseStatus").value("COMING_SOON"))
                .andExpect(jsonPath("$.items[1].enableable").value(false))
                .andExpect(jsonPath("$.items[1].name").value("Dossiers de santé"));
    }

    @Test
    void englishAcceptLanguageReturnsEnglishLabels() throws Exception {
        mockMvc.perform(get("/api/v1/platform/features")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Animal management"));
    }

    @Test
    void catalogSeedIsIdempotent() {
        String sql = """
                INSERT INTO feature_catalog (
                    id, code, name_ar, name_en, name_fr, icon_code, release_status, display_order, active,
                    created_at, created_by, updated_at, updated_by
                )
                SELECT
                    'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
                    'ANIMAL_MANAGEMENT',
                    'إدارة الحيوانات',
                    'Animal management',
                    'Gestion des animaux',
                    'paw',
                    'AVAILABLE',
                    10,
                    TRUE,
                    CURRENT_TIMESTAMP,
                    'system',
                    CURRENT_TIMESTAMP,
                    'system'
                WHERE NOT EXISTS (
                    SELECT 1 FROM feature_catalog WHERE code = 'ANIMAL_MANAGEMENT'
                )
                """;
        jdbcTemplate.update(sql);
        jdbcTemplate.update(sql);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feature_catalog WHERE code = 'ANIMAL_MANAGEMENT'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void comingSoonFeatureCannotBeEnabled() throws Exception {
        UUID healthId = featureCatalogRepository.findByCodeIgnoreCase("HEALTH").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/features", harriId())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content("{\"featureId\":\"" + healthId + "\",\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FEATURE_NOT_AVAILABLE))
                .andExpect(jsonPath("$.message").value("This feature is not available to enable."));
    }

    @Test
    void inactiveAndRetiredFeaturesCannotBeEnabled() throws Exception {
        FeatureCatalog inactive = featureCatalogRepository.saveAndFlush(new FeatureCatalog(
                "INACTIVE_MOD", "معطل", "Inactive", "Inactif", FeatureReleaseStatus.AVAILABLE, 90));
        inactive.setActive(false);
        featureCatalogRepository.saveAndFlush(inactive);

        FeatureCatalog retired = featureCatalogRepository.saveAndFlush(new FeatureCatalog(
                "RETIRED_MOD", "متقاعد", "Retired", "Retire", FeatureReleaseStatus.RETIRED, 91));

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/features", harriId())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featureId\":\"" + inactive.getId() + "\",\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FEATURE_NOT_AVAILABLE));

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/features", harriId())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featureId\":\"" + retired.getId() + "\",\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FEATURE_NOT_AVAILABLE));
    }

    @Test
    void availableFeatureCanBeAssignedWithoutFarmFlagColumns() throws Exception {
        UUID animalId = featureCatalogRepository.findByCodeIgnoreCase("ANIMAL_MANAGEMENT").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/features", harriId())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featureId\":\"" + animalId + "\",\"enabled\":true,\"configurationJson\":\"{\\\"limit\\\":10}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureCode").value("ANIMAL_MANAGEMENT"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.farmId").value(harriId().toString()));
    }

    @Test
    void invalidConfigurationJsonIsRejected() throws Exception {
        UUID animalId = featureCatalogRepository.findByCodeIgnoreCase("ANIMAL_MANAGEMENT").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/features", harriId())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featureId\":\"" + animalId + "\",\"enabled\":true,\"configurationJson\":\"[]\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FEATURE_ASSIGNMENT_INVALID));
    }

    @Test
    void unknownFeatureReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/features", harriId())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"featureId\":\"" + UUID.randomUUID() + "\",\"enabled\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FEATURE_NOT_FOUND));
    }

    @Test
    void farmOwnerCannotReadTheCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/platform/features")
                        .with(JwtAuth.withPermissions(Permission.BREED_MANAGE.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    private UUID harriId() {
        return farmRepository.findByCodeIgnoreCase("HARRI").orElseThrow().getId();
    }
}
