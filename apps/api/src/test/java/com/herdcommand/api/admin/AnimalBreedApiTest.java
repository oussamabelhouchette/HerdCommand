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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class AnimalBreedApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-breeds").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void listRequiresViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-breeds")
                        .with(JwtAuth.authenticated())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    @Test
    void createRequiresManagePermission() throws Exception {
        mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("VIEWONLY", "عواسي", "Awassi")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createNormalizesCodeAndReturnsAuditFields() throws Exception {
        String suffix = unique();
        MvcResult result = mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("  awassi_" + suffix + "  ", "عواسي", "Awassi")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("AWASSI_" + suffix))
                .andExpect(jsonPath("$.nameAr").value("عواسي"))
                .andExpect(jsonPath("$.nameEn").value("Awassi"))
                .andExpect(jsonPath("$.speciesCode").value("SHEEP"))
                .andExpect(jsonPath("$.displayOrder").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.createdBy").value("user-1"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.updatedBy").value("user-1"))
                .andReturn();

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/v1/admin/animal-breeds/{id}", id)
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AWASSI_" + suffix));
    }

    @Test
    void duplicateCodeReturnsConflict() throws Exception {
        String code = "NAJDI_" + unique();
        mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(code, "نجدي", "Najdi")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(code.toLowerCase(), "نجدي 2", "Najdi 2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.BREED_CODE_ALREADY_EXISTS))
                .andExpect(jsonPath("$.message").value("A breed with this code already exists."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-breeds"));
    }

    @Test
    void invalidCreateReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"nameAr\":\"\",\"nameEn\":\"\",\"speciesCode\":\"SHEEP\",\"displayOrder\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/animal-breeds"));
    }

    @Test
    void putRejectsCodeChangeAndLeavesStoredCode() throws Exception {
        String suffix = unique();
        String id = createBreed("HARB_" + suffix, "حربي", "Harbi");

        mockMvc.perform(put("/api/v1/admin/animal-breeds/{id}", id)
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CHANGED_%s",
                                  "nameAr": "حربي محدث",
                                  "nameEn": "Harbi updated",
                                  "speciesCode": "SHEEP",
                                  "displayOrder": 2
                                }
                                """.formatted(suffix)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));

        mockMvc.perform(get("/api/v1/admin/animal-breeds/{id}", id)
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HARB_" + suffix))
                .andExpect(jsonPath("$.nameEn").value("Harbi"));
    }

    @Test
    void putUpdatesEditableFieldsWhenCodeUnchanged() throws Exception {
        String suffix = unique();
        String id = createBreed("NUAIMI_" + suffix, "نعيمي", "Naimi");

        mockMvc.perform(put("/api/v1/admin/animal-breeds/{id}", id)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "nuaimi_%s",
                                  "nameAr": "نعيمي محدث",
                                  "nameEn": "Naimi updated",
                                  "speciesCode": "GOAT",
                                  "displayOrder": 5
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NUAIMI_" + suffix))
                .andExpect(jsonPath("$.nameEn").value("Naimi updated"))
                .andExpect(jsonPath("$.speciesCode").value("GOAT"))
                .andExpect(jsonPath("$.displayOrder").value(5));
    }

    @Test
    void patchDeactivatesBreedAndKeepsLabel() throws Exception {
        String suffix = unique();
        String id = createBreed("BARQI_" + suffix, "برقي", "Barqi");

        mockMvc.perform(patch("/api/v1/admin/animal-breeds/{id}/status", id)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.nameAr").value("برقي"))
                .andExpect(jsonPath("$.code").value("BARQI_" + suffix));
    }

    @Test
    void searchFiltersBySpeciesAndText() throws Exception {
        String suffix = unique();
        createBreed("SHEEPQ_" + suffix, "بحث غنم", "SearchSheep");
        createGoat("GOATQ_" + suffix, "بحث ماعز", "SearchGoat");

        mockMvc.perform(get("/api/v1/admin/animal-breeds")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .param("search", "SearchSheep")
                        .param("speciesCode", "SHEEP")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "displayOrder,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].nameEn").value("SearchSheep"));
    }

    @Test
    void missingBreedReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-breeds/{id}", UUID.randomUUID())
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor manageAndView() {
        return JwtAuth.withPermissions(Permission.BREED_MANAGE.name(), Permission.ANIMAL_CONFIG_VIEW.name());
    }

    private String createBreed(String code, String nameAr, String nameEn) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(code, nameAr, nameEn)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private void createGoat(String code, String nameAr, String nameEn) throws Exception {
        mockMvc.perform(post("/api/v1/admin/animal-breeds")
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "nameAr": "%s",
                                  "nameEn": "%s",
                                  "speciesCode": "GOAT",
                                  "displayOrder": 0
                                }
                                """.formatted(code, nameAr, nameEn)))
                .andExpect(status().isCreated());
    }

    private static String createJson(String code, String nameAr, String nameEn) {
        return """
                {
                  "code": "%s",
                  "nameAr": "%s",
                  "nameEn": "%s",
                  "speciesCode": "SHEEP"
                }
                """.formatted(code, nameAr, nameEn);
    }

    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
