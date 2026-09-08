package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.identity.IdentityDirectory;
import com.herdcommand.api.domain.identity.IdentityProviderException;
import com.herdcommand.api.domain.identity.IdentityRecord;
import com.herdcommand.api.security.Permission;
import com.herdcommand.api.support.JwtAuth;
import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class IdentityLookupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityDirectory identityDirectory;

    @Test
    void existingEnabledUserIsReturnedSafely() throws Exception {
        when(identityDirectory.findByExactEmail("owner@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-1", "owner@example.tn", "Mohamed Ben Salem", true)));

        mockMvc.perform(post("/api/v1/platform/identity/lookup")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  Owner@Example.TN \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.invitationRequired").value(false))
                .andExpect(jsonPath("$.keycloakUserId").value("kc-1"))
                .andExpect(jsonPath("$.email").value("owner@example.tn"))
                .andExpect(jsonPath("$.displayName").value("Mohamed Ben Salem"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.credentials").doesNotExist())
                .andExpect(jsonPath("$.realmRoles").doesNotExist());
    }

    @Test
    void missingUserIsInvitationRequiredNotAnError() throws Exception {
        when(identityDirectory.findByExactEmail("new@example.tn")).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/platform/identity/lookup")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.tn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.invitationRequired").value(true))
                .andExpect(jsonPath("$.email").value("new@example.tn"));
    }

    @Test
    void disabledUserIsReturnedSoTheUiCanBlockSelection() throws Exception {
        when(identityDirectory.findByExactEmail("off@example.tn")).thenReturn(List.of(
                new IdentityRecord("kc-off", "off@example.tn", "Disabled", false)));

        mockMvc.perform(post("/api/v1/platform/identity/lookup")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"off@example.tn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void keycloakOutageReturnsUnavailable() throws Exception {
        when(identityDirectory.findByExactEmail("owner@example.tn"))
                .thenThrow(new IdentityProviderException("down"));

        mockMvc.perform(post("/api/v1/platform/identity/lookup")
                        .with(JwtAuth.withPermissions(Permission.PLATFORM_ADMIN.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content("{\"email\":\"owner@example.tn\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE));
    }

    @Test
    void farmOwnerCannotLookupIdentities() throws Exception {
        mockMvc.perform(post("/api/v1/platform/identity/lookup")
                        .with(JwtAuth.withPermissions(Permission.BREED_MANAGE.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@example.tn\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }
}
