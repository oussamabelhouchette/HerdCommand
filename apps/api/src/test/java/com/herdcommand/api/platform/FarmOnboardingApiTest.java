package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.Farm;
import com.herdcommand.api.domain.farm.FarmMembershipRepository;
import com.herdcommand.api.domain.farm.FarmMembershipStatus;
import com.herdcommand.api.domain.farm.FarmOnboardingCompensationRepository;
import com.herdcommand.api.domain.farm.FarmPlanCode;
import com.herdcommand.api.domain.farm.FarmPlanLimits;
import com.herdcommand.api.domain.farm.FarmRepository;
import com.herdcommand.api.domain.farm.FarmStatus;
import com.herdcommand.api.domain.farm.FarmSubscriptionRepository;
import com.herdcommand.api.domain.feature.FarmFeatureRepository;
import com.herdcommand.api.domain.identity.IdentityDirectory;
import com.herdcommand.api.domain.identity.IdentityProviderException;
import com.herdcommand.api.domain.identity.IdentityRecord;
import com.herdcommand.api.security.Permission;
import com.herdcommand.api.support.JwtAuth;
import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class FarmOnboardingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private FarmRepository farmRepository;

    @Autowired
    private FarmMembershipRepository membershipRepository;

    @Autowired
    private FarmSubscriptionRepository subscriptionRepository;

    @Autowired
    private FarmFeatureRepository farmFeatureRepository;

    @Autowired
    private FarmOnboardingCompensationRepository compensationRepository;

    @MockBean
    private IdentityDirectory identityDirectory;

    @AfterEach
    void resetFarmSpy() {
        reset(farmRepository);
    }

    @Test
    void createsFarmForExistingIdentity() throws Exception {
        when(identityDirectory.findByExactEmail("owner@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-existing", "owner@example.tn", "Mohamed Ben Salem", true)));
        long farmsBefore = farmRepository.count();

        String body = farmJson("مزرعة النخيل", "Ferme des Palmiers", "owner@example.tn", "Mohamed Ben Salem",
                List.of("ANIMAL_MANAGEMENT"), FarmPlanLimits.TRIAL_MAX_ANIMALS, FarmPlanLimits.TRIAL_MAX_TEAM);

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value(FarmStatus.SETUP.name()))
                .andExpect(jsonPath("$.ownerMembershipStatus").value(FarmMembershipStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.invitationEmailSent").value(false))
                .andExpect(jsonPath("$.enabledFeatureCodes[0]").value("ANIMAL_MANAGEMENT"))
                .andDo(result -> {
                    String id = json(result, "id");
                    UUID farmId = UUID.fromString(id);
                    Farm farm = farmRepository.findById(farmId).orElseThrow();
                    assertThat(farm.getCode()).startsWith("FARM-TN-");
                    assertThat(farm.getCreatedBy()).isEqualTo("user-1");
                    assertThat(farm.getNameEn()).isEqualTo("Ferme des Palmiers");
                    assertThat(membershipRepository.findByFarmIdAndKeycloakUserId(farmId, "kc-existing"))
                            .get()
                            .extracting(m -> m.getStatus())
                            .isEqualTo(FarmMembershipStatus.ACTIVE);
                    assertThat(subscriptionRepository.findByFarmId(farmId).orElseThrow().getMaxActiveAnimals())
                            .isEqualTo(FarmPlanLimits.TRIAL_MAX_ANIMALS);
                    assertThat(farmFeatureRepository.findByFarmId(farmId))
                            .extracting(feature -> feature.isEnabled())
                            .containsExactly(true);
                    verify(identityDirectory, never()).createInvitedUser(any(), any());
                });

        assertThat(farmRepository.count()).isEqualTo(farmsBefore + 1);
    }

    @Test
    void createsFarmForMissingIdentityAsInvited() throws Exception {
        when(identityDirectory.findByExactEmail("new-owner@example.tn")).thenReturn(List.of());
        when(identityDirectory.createInvitedUser("new-owner@example.tn", "New Owner"))
                .thenReturn(new IdentityRecord("kc-new", "new-owner@example.tn", "New Owner", true));

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(farmJson("مزرعة جديدة", "Ferme nouvelle", "new-owner@example.tn", "New Owner",
                                List.of("ANIMAL_MANAGEMENT"), null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerMembershipStatus").value(FarmMembershipStatus.INVITED.name()))
                .andExpect(jsonPath("$.invitationEmailSent").value(true));

        verify(identityDirectory).sendExecuteActionsEmail("kc-new");
    }

    @Test
    void rejectsComingSoonFeatureWithoutCreatingAnything() throws Exception {
        long farmsBefore = farmRepository.count();

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(farmJson("مزرعة صحة", "Ferme sante", "health@example.tn", "Health",
                                List.of("HEALTH"), null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FEATURE_NOT_AVAILABLE));

        assertThat(farmRepository.count()).isEqualTo(farmsBefore);
        verify(identityDirectory, never()).findByExactEmail(any());
        verify(identityDirectory, never()).createInvitedUser(any(), any());
    }

    @Test
    void rejectsClientTrialLimitsAboveTheCatalog() throws Exception {
        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(farmJson("حدود", "Limites", "limits@example.tn", "Limits",
                                List.of(), 300, 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PLAN_LIMIT_INVALID));

        verify(identityDirectory, never()).findByExactEmail(any());
    }

    @Test
    void keycloakOutageCreatesNoFarm() throws Exception {
        when(identityDirectory.findByExactEmail("down@example.tn"))
                .thenThrow(new IdentityProviderException("down"));
        long farmsBefore = farmRepository.count();

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(farmJson("تعطل", "Panne", "down@example.tn", "Down", List.of(), null, null)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE));

        assertThat(farmRepository.count()).isEqualTo(farmsBefore);
    }

    @Test
    void databaseFailureCompensatesCreatedIdentity() throws Exception {
        when(identityDirectory.findByExactEmail("compensate@example.tn")).thenReturn(List.of());
        when(identityDirectory.createInvitedUser("compensate@example.tn", "Compensate"))
                .thenReturn(new IdentityRecord("kc-compensate", "compensate@example.tn", "Compensate", true));
        doThrow(new DataIntegrityViolationException("forced onboarding failure"))
                .when(farmRepository).saveAndFlush(any(Farm.class));
        long farmsBefore = farmRepository.count();

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(farmJson("تعويض", "Compensation", "compensate@example.tn", "Compensate",
                                List.of(), null, null)))
                .andExpect(status().isConflict());

        assertThat(farmRepository.count()).isEqualTo(farmsBefore);
        verify(identityDirectory).deleteCreatedIdentity("kc-compensate");
        verify(identityDirectory, never()).deleteCreatedIdentity("kc-existing");
    }

    @Test
    void failedCompensationIsRecordedForRetry() throws Exception {
        when(identityDirectory.findByExactEmail("stuck@example.tn")).thenReturn(List.of());
        when(identityDirectory.createInvitedUser("stuck@example.tn", "Stuck"))
                .thenReturn(new IdentityRecord("kc-stuck", "stuck@example.tn", "Stuck", true));
        doThrow(new DataIntegrityViolationException("forced onboarding failure"))
                .when(farmRepository).saveAndFlush(any(Farm.class));
        doThrow(new IdentityProviderException("cannot delete"))
                .when(identityDirectory).deleteCreatedIdentity("kc-stuck");

        String key = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(farmJson("عالق", "Bloque", "stuck@example.tn", "Stuck", List.of(), null, null)))
                .andExpect(status().isConflict());

        assertThat(compensationRepository.findByIdempotencyKey(key))
                .extracting(row -> row.getKeycloakUserId())
                .containsExactly("kc-stuck");
    }

    @Test
    void sameIdempotencyKeyReplaysTheOriginalFarm() throws Exception {
        when(identityDirectory.findByExactEmail("retry@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-retry", "retry@example.tn", "Retry", true)));
        String key = UUID.randomUUID().toString();
        String body = farmJson("إعادة", "Retry", "retry@example.tn", "Retry", List.of(), null, null);
        long farmsBefore = farmRepository.count();

        MvcResult first = mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(json(first, "id")))
                .andExpect(jsonPath("$.code").value(json(first, "code")));

        assertThat(farmRepository.count()).isEqualTo(farmsBefore + 1);
    }

    @Test
    void sameKeyDifferentBodyConflicts() throws Exception {
        when(identityDirectory.findByExactEmail("conflict@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-conflict", "conflict@example.tn", "Conflict", true)));
        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(farmJson("أولى", "First", "conflict@example.tn", "Conflict", List.of(), null, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(farmJson("ثانية", "Second", "conflict@example.tn", "Conflict", List.of(), null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void concurrentRetriesShareOneFarm() throws Exception {
        when(identityDirectory.findByExactEmail("race@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-race", "race@example.tn", "Race", true)));
        String key = UUID.randomUUID().toString();
        String body = farmJson("سباق", "Course", "race@example.tn", "Race", List.of(), null, null);
        long farmsBefore = farmRepository.count();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> task = () -> mockMvc.perform(post("/api/v1/platform/farms")
                            .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", key)
                            .content(body))
                    .andReturn();
            Future<MvcResult> first = pool.submit(task);
            Future<MvcResult> second = pool.submit(task);
            MvcResult a = first.get();
            MvcResult b = second.get();
            assertThat(a.getResponse().getStatus()).isEqualTo(201);
            assertThat(b.getResponse().getStatus()).isEqualTo(201);
            assertThat(json(a, "id")).isEqualTo(json(b, "id"));
        } finally {
            pool.shutdownNow();
        }

        assertThat(farmRepository.count()).isEqualTo(farmsBefore + 1);
    }

    @Test
    void missingIdempotencyKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(farmJson("مفتاح", "Cle", "key@example.tn", "Key", List.of(), null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED));
    }

    @Test
    void farmOwnerCannotCreateFarms() throws Exception {
        mockMvc.perform(post("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.BREED_MANAGE.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(farmJson("ممنوع", "Interdit", "forbidden@example.tn", "No", List.of(), null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    private static String farmJson(
            String nameAr,
            String nameFr,
            String email,
            String displayName,
            List<String> features,
            Integer animals,
            Integer team) {
        String featureJson = features.stream()
                .map(code -> "\"" + code + "\"")
                .reduce((a, b) -> a + "," + b)
                .map(joined -> "[" + joined + "]")
                .orElse("[]");
        String animalsJson = animals == null ? "null" : animals.toString();
        String teamJson = team == null ? "null" : team.toString();
        return """
                {
                  "nameAr": "%s",
                  "nameFr": "%s",
                  "governorateCode": "TN-11",
                  "timezone": "Africa/Tunis",
                  "defaultLanguage": "ar",
                  "currencyCode": "TND",
                  "initialStatus": "SETUP",
                  "owner": {
                    "email": "%s",
                    "displayName": "%s",
                    "phoneNumber": "+21620000000"
                  },
                  "subscription": {
                    "planCode": "%s",
                    "maxActiveAnimals": %s,
                    "maxTeamMembers": %s
                  },
                  "enabledFeatureCodes": %s
                }
                """.formatted(nameAr, nameFr, email, displayName, FarmPlanCode.TRIAL.name(), animalsJson, teamJson, featureJson);
    }

    private static String json(MvcResult result, String field) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$." + field);
    }
}
