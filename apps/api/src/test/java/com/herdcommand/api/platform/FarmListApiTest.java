package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.AssignFarmOwnerCommand;
import com.herdcommand.api.domain.farm.CreateFarmCommand;
import com.herdcommand.api.domain.farm.FarmRepository;
import com.herdcommand.api.domain.farm.FarmStatus;
import com.herdcommand.api.domain.farm.FarmTenantService;
import com.herdcommand.api.domain.farm.FarmTenantSnapshot;
import com.herdcommand.api.domain.farm.PlatformFarmQueryService;
import com.herdcommand.api.domain.feature.FarmFeatureService;
import com.herdcommand.api.security.Permission;
import com.herdcommand.api.support.JwtAuth;
import com.herdcommand.api.support.TestJwtConfig;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class FarmListApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmTenantService farmTenantService;

    @Autowired
    private FarmFeatureService farmFeatureService;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private FarmTenantSnapshot namedFarm;
    private FarmTenantSnapshot activeOwnedFarm;

    @BeforeEach
    void setUp() {
        authenticateAuditor();
        namedFarm = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة النخيل", "Palm Farm", "Ferme des Palmiers", "TN-11", null, null, "ar"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                namedFarm.id(), "kc-palm", "palm@example.tn", "Mohamed Ben Salem"));
        farmFeatureService.enableByCodes(namedFarm.id(), List.of("ANIMAL_MANAGEMENT"));

        activeOwnedFarm = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة النشاط", "Active Farm", "Ferme active", "TN-12", null, null, "fr"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                activeOwnedFarm.id(), "kc-active", "active-owner@example.tn", "Sami Trabelsi"));
        farmTenantService.activate(activeOwnedFarm.id());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchesByNameCodeAndOwner() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("search", "النخيل")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(namedFarm.id().toString()))
                .andExpect(jsonPath("$.items[0].name").value("مزرعة النخيل"))
                .andExpect(jsonPath("$.items[0].ownerEmail").value("palm@example.tn"))
                .andExpect(jsonPath("$.items[0].ownerDisplayName").value("Mohamed Ben Salem"))
                .andExpect(jsonPath("$.items[0].enabledFeatureCodes[0]").value("ANIMAL_MANAGEMENT"))
                .andExpect(jsonPath("$.items[0].activeAnimalCount").value(0));

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("search", namedFarm.code())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value(namedFarm.code()));

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("search", "PALM@EXAMPLE")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(namedFarm.id().toString()));

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("search", "Ben Salem")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ownerDisplayName").value("Mohamed Ben Salem"));
    }

    @Test
    void combinesStatusAndPlanFilters() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("status", "ACTIVE")
                        .param("planCode", "ESSENTIAL")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code").value("HARRI"))
                .andExpect(jsonPath("$.items[0].status").value(FarmStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.items[0].planCode").value("ESSENTIAL"))
                .andDo(result -> {
                    int length = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.items.length()");
                    for (int i = 0; i < length; i++) {
                        String status = com.jayway.jsonpath.JsonPath.read(
                                result.getResponse().getContentAsString(), "$.items[" + i + "].status");
                        String plan = com.jayway.jsonpath.JsonPath.read(
                                result.getResponse().getContentAsString(), "$.items[" + i + "].planCode");
                        assertThat(status).isEqualTo("ACTIVE");
                        assertThat(plan).isEqualTo("ESSENTIAL");
                    }
                });
    }

    @Test
    void filtersByEnabledFeature() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("featureCode", "ANIMAL_MANAGEMENT")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].enabledFeatureCodes[0]").value("ANIMAL_MANAGEMENT"));
    }

    @Test
    void pagesAndSortsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(activeOwnedFarm.id().toString()))
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void emptySearchAndInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("search", "no-such-farm-zzzz")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("status", "NOPE")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("planCode", "ENTERPRISE")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PLAN_NOT_FOUND));

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("sort", "planCode,desc")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
    }

    @Test
    void detailsAndSummary() throws Exception {
        UUID harriId = farmRepository.findByCodeIgnoreCase("HARRI").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/platform/farms/{farmId}", namedFarm.id())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(namedFarm.id().toString()))
                .andExpect(jsonPath("$.name").value("Palm Farm"))
                .andExpect(jsonPath("$.ownerMembershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.planCode").value("TRIAL"))
                .andExpect(jsonPath("$.createdBy").value("platform-admin-1"));

        mockMvc.perform(get("/api/v1/platform/farms/{farmId}", harriId)
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HARRI"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/platform/farms/{farmId}", UUID.randomUUID())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND));

        mockMvc.perform(get("/api/v1/platform/farms/summary")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFarms").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.activeFarms").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.setupFarms").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeAnimals").value(0));
    }

    @Test
    void listUsesBoundedQueriesForAPage() throws Exception {
        for (int i = 0; i < 8; i++) {
            farmTenantService.createFarm(new CreateFarmCommand(
                    "مزرعة " + i, "Farm " + i, "Ferme " + i, "TN-11", null, null, "ar"));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mockMvc.perform(get("/api/v1/platform/farms")
                        .param("size", "20")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(10)));

        assertThat(statistics.getPrepareStatementCount())
                .as("page + count + owner/subscription/feature batches")
                .isLessThanOrEqualTo(10);
    }

    @Test
    void pageSizeIsCapped() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 500);
        var sanitized = PlatformFarmQueryService.sanitize(pageable);
        assertThat(sanitized.getPageSize()).isEqualTo(PlatformFarmQueryService.MAX_PAGE_SIZE);
    }

    @Test
    void farmOwnerCannotListPlatformFarms() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.BREED_MANAGE.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    private static void authenticateAuditor() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("platform-admin-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
