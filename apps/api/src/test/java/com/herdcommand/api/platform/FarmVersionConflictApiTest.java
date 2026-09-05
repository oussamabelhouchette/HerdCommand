package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.CreateFarmCommand;
import com.herdcommand.api.domain.farm.FarmTenantService;
import com.herdcommand.api.domain.farm.FarmTenantSnapshot;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class FarmVersionConflictApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmTenantService farmTenantService;

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
    void staleFarmVersionReturnsConflict() throws Exception {
        FarmTenantSnapshot farm = farmTenantService.createFarm(new CreateFarmCommand(
                "تعارض", "Conflict", "Conflit", "TN-11", null, null, "ar"));

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/version-check", farm.id())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content("{\"version\":" + farm.version() + ",\"nameAr\":\"تحديث أول\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(farm.version() + 1));

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/version-check", farm.id())
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content("{\"version\":" + farm.version() + ",\"nameAr\":\"تحديث قديم\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FARM_VERSION_CONFLICT))
                .andExpect(jsonPath("$.path").value("/api/v1/platform/farms/" + farm.id() + "/version-check"))
                .andExpect(jsonPath("$.message").value("This farm was updated by someone else. Reload and try again."));
    }

    @Test
    void farmOwnerCannotUseVersionProbe() throws Exception {
        FarmTenantSnapshot farm = farmTenantService.createFarm(new CreateFarmCommand(
                "ممنوع", "Forbidden", "Interdit", "TN-12", null, null, "ar"));

        mockMvc.perform(post("/api/v1/platform/farms/{farmId}/version-check", farm.id())
                        .with(JwtAuth.withPermissions("BREED_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"nameAr\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }
}
