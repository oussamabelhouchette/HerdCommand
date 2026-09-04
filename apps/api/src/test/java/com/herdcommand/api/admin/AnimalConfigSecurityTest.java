package com.herdcommand.api.admin;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class AnimalConfigSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedAdminAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-config").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-config"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void authenticatedCallerWithoutPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-config")
                        .with(JwtAuth.authenticated())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-config"));
    }

    @Test
    void callerWithViewPermissionCanReadFoundation() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-config")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLocale").value("ar"))
                .andExpect(jsonPath("$.permissions[0]").value("ANIMAL_CONFIG_VIEW"));
    }
}
