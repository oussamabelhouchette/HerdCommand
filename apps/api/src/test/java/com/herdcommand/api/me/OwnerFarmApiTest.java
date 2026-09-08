package com.herdcommand.api.me;

import com.herdcommand.api.domain.farm.AssignFarmOwnerCommand;
import com.herdcommand.api.domain.farm.CreateFarmCommand;
import com.herdcommand.api.domain.farm.FarmTenantService;
import com.herdcommand.api.domain.farm.FarmTenantSnapshot;
import com.herdcommand.api.domain.feature.FarmFeatureService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
@Transactional
class OwnerFarmApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmTenantService farmTenantService;

    @Autowired
    private FarmFeatureService farmFeatureService;

    private FarmTenantSnapshot firstFarm;
    private FarmTenantSnapshot secondFarm;
    private FarmTenantSnapshot otherOwnerFarm;

    @BeforeEach
    void setUp() {
        authenticateAuditor();
        firstFarm = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة الأولى", "First Farm", "Première ferme", "TN-11", "Tunis", null, "ar"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                firstFarm.id(), "kc-owner-1", "owner1@example.tn", "Owner One"));
        farmFeatureService.enableByCodes(firstFarm.id(), List.of("ANIMAL_MANAGEMENT"));

        secondFarm = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة الثانية", "Second Farm", "Deuxième ferme", "TN-12", null, null, "ar"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                secondFarm.id(), "kc-owner-1", "owner1@example.tn", "Owner One"));

        otherOwnerFarm = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة الآخر", "Other Farm", "Autre ferme", "TN-13", null, null, "ar"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                otherOwnerFarm.id(), "kc-owner-2", "owner2@example.tn", "Owner Two"));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsOnlyActiveOwnedFarmsAndFeatureFlag() throws Exception {
        mockMvc.perform(get("/api/v1/me/farms")
                        .with(JwtAuth.authenticatedAs("kc-owner-1"))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(firstFarm.id().toString()))
                .andExpect(jsonPath("$[0].name").value("First Farm"))
                .andExpect(jsonPath("$[0].animalManagementEnabled").value(true))
                .andExpect(jsonPath("$[0].enabledFeatureCodes[0]").value("ANIMAL_MANAGEMENT"))
                .andExpect(jsonPath("$[1].id").value(secondFarm.id().toString()))
                .andExpect(jsonPath("$[1].animalManagementEnabled").value(false));
    }

    @Test
    void doesNotLeakAnotherOwnerFarm() throws Exception {
        mockMvc.perform(get("/api/v1/me/farms")
                        .with(JwtAuth.authenticatedAs("kc-owner-2"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherOwnerFarm.id().toString()));
    }

    @Test
    void emptyWhenCallerOwnsNothing() throws Exception {
        mockMvc.perform(get("/api/v1/me/farms")
                        .with(JwtAuth.authenticatedAs("kc-nobody"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me/farms").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
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
