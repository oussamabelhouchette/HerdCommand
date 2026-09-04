package com.herdcommand.api;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.ErrorCodes;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class ApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void meRequiresAuthenticationWithJsonEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/me"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void meReturnsSubjectFromJwt() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwt -> jwt
                        .subject("user-1")
                        .claim("preferred_username", "farmer")
                        .claim("email", "farmer@herdcommand.local")
                        .claim("realm_access", Map.of("roles", List.of("manager"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-1"))
                .andExpect(jsonPath("$.username").value("farmer"))
                .andExpect(jsonPath("$.roles[0]").value("manager"));
    }

    @Test
    void apiErrorShapeIsStable() {
        ApiError error = ApiError.of("VALIDATION_ERROR", "The request contains invalid fields.", "/api/v1/example");
        org.assertj.core.api.Assertions.assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        org.assertj.core.api.Assertions.assertThat(error.path()).isEqualTo("/api/v1/example");
        org.assertj.core.api.Assertions.assertThat(error.fieldErrors()).isEmpty();
    }
}
