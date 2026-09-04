package com.herdcommand.api.error;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class ValidationErrorSerializationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidFieldsReturnStandardValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/animal-config/validation-check")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en")
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("The request contains invalid fields."))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-config/validation-check"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("This field is required."));
    }

    @Test
    void notFoundUsesStandardEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-config/not-found-check")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-config/not-found-check"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void conflictUsesStandardEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-config/conflict-check")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.BREED_CODE_ALREADY_EXISTS))
                .andExpect(jsonPath("$.message").value("A breed with this code already exists."))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-config/conflict-check"));
    }
}
