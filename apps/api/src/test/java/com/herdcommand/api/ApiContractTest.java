package com.herdcommand.api;

import com.herdcommand.api.api.error.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiContractTest.TestJwtConfig.class)
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
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.correlationId").exists())
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
        ApiError error = ApiError.of("validation_error", "Request validation failed", "cid", "/api/v1/example");
        org.assertj.core.api.Assertions.assertThat(error.code()).isEqualTo("validation_error");
        org.assertj.core.api.Assertions.assertThat(error.path()).isEqualTo("/api/v1/example");
        org.assertj.core.api.Assertions.assertThat(error.errors()).isEmpty();
    }

    @TestConfiguration
    static class TestJwtConfig {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("user-1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .claim("preferred_username", "farmer")
                    .build();
        }
    }
}
