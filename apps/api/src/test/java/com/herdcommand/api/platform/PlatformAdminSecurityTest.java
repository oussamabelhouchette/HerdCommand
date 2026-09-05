package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.security.Permission;
import com.herdcommand.api.support.JwtAuth;
import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class PlatformAdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedPlatformAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/platform/farms"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void farmOwnerWithoutPlatformAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .with(jwt().jwt(token -> token
                                .subject("owner-1")
                                .claim("realm_access", Map.of("roles", List.of("owner")))))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN))
                .andExpect(jsonPath("$.path").value("/api/v1/platform/farms"))
                .andExpect(jsonPath("$.farms").doesNotExist());
    }

    @Test
    void authenticatedCallerWithoutPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform")
                        .with(JwtAuth.authenticated())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN))
                .andExpect(jsonPath("$.path").value("/api/v1/platform"));
    }

    @Test
    void platformAdminCanReadFoundation() throws Exception {
        mockMvc.perform(get("/api/v1/platform")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLocale").value("ar"))
                .andExpect(jsonPath("$.permissions[0]").value("PLATFORM_ADMIN"));
    }

    @Test
    void platformAdminCanAccessFarmsPath() throws Exception {
        mockMvc.perform(get("/api/v1/platform/farms")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessible").value(true))
                .andExpect(jsonPath("$.farms").isEmpty());
    }
}
