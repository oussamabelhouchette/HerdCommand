package com.herdcommand.api.admin;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.FarmRepository;
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
class AnimalGroupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/animal-groups", seededFarmId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void listRequiresViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/animal-groups", seededFarmId())
                        .with(JwtAuth.authenticated())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    @Test
    void createRequiresManagePermission() throws Exception {
        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", seededFarmId())
                        .with(JwtAuth.withPermissions(Permission.GROUP_VIEW.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("VIEWONLY", "أمهات", "Dams")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOnSeededFarmReturnsAuditAndZeroAnimalCount() throws Exception {
        String suffix = unique();
        String farmId = seededFarmId();

        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", farmId)
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson("  dams_" + suffix + "  ", "أمهات", "Dams")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.farmId").value(farmId))
                .andExpect(jsonPath("$.code").value("DAMS_" + suffix))
                .andExpect(jsonPath("$.nameAr").value("أمهات"))
                .andExpect(jsonPath("$.nameEn").value("Dams"))
                .andExpect(jsonPath("$.groupTypeCode").value("DAMS"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.animalCount").value(0))
                .andExpect(jsonPath("$.createdBy").value("user-1"))
                .andExpect(jsonPath("$.updatedBy").value("user-1"));
    }

    @Test
    void listFarmsReturnsSeededHarri() throws Exception {
        mockMvc.perform(get("/api/v1/farms")
                        .with(JwtAuth.withPermissions(Permission.GROUP_VIEW.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='HARRI')]").exists());
    }

    @Test
    void duplicateCodeOnSameFarmConflicts() throws Exception {
        String farmId = seededFarmId();
        String code = "DAMS_" + unique();
        createGroup(farmId, code, "أمهات", "Dams");

        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", farmId)
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(code.toLowerCase(), "أمهات 2", "Dams 2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.GROUP_CODE_ALREADY_EXISTS))
                .andExpect(jsonPath("$.message").value("A group with this code already exists on this farm."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    }

    @Test
    void sameCodeOnSecondFarmIsAllowed() throws Exception {
        String code = "SHARED_" + unique();
        String farmA = seededFarmId();
        String farmB = otherFarmId();
        createGroup(farmA, code, "أمهات أ", "Dams A");

        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", farmB)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(code, "أمهات ب", "Dams B")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmId").value(farmB))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void crossFarmGetAndUpdateReturnNotFound() throws Exception {
        String farmA = seededFarmId();
        String farmB = otherFarmId();
        String groupId = createGroup(farmA, "ISO_" + unique(), "عزل", "Isolation");

        mockMvc.perform(get("/api/v1/farms/{farmId}/animal-groups/{groupId}", farmB, groupId)
                        .with(JwtAuth.withPermissions(Permission.GROUP_VIEW.name()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND));

        mockMvc.perform(put("/api/v1/farms/{farmId}/animal-groups/{groupId}", farmB, groupId)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("CHANGED", "عزل محدث", "Isolation updated")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/farms/{farmId}/animal-groups/{groupId}", farmA, groupId)
                        .with(JwtAuth.withPermissions(Permission.GROUP_VIEW.name())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameEn").value("Isolation"));
    }

    @Test
    void unknownFarmReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/animal-groups", UUID.randomUUID())
                        .with(JwtAuth.withPermissions(Permission.GROUP_VIEW.name()))
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND));
    }

    @Test
    void invalidCreateReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", seededFarmId())
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "nameAr": "",
                                  "nameEn": "",
                                  "groupTypeCode": "DAMS",
                                  "capacity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void unknownGroupTypeReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", seededFarmId())
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BADTYPE",
                                  "nameAr": "خطأ",
                                  "nameEn": "Bad",
                                  "groupTypeCode": "PENS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
    }

    @Test
    void listAnimalCountIsZeroAndSearchWorks() throws Exception {
        String suffix = unique();
        String farmId = seededFarmId();
        createGroup(farmId, "YOUNG_" + suffix, "صغار بحث", "SearchYoung");

        mockMvc.perform(get("/api/v1/farms/{farmId}/animal-groups", farmId)
                        .with(JwtAuth.withPermissions(Permission.GROUP_VIEW.name()))
                        .param("search", "SearchYoung")
                        .param("groupTypeCode", "YOUNG")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[0].nameEn").value("SearchYoung"))
                .andExpect(jsonPath("$.items[0].animalCount").value(0));
    }

    @Test
    void patchDeactivatesAndActivates() throws Exception {
        String farmId = seededFarmId();
        String id = createGroup(farmId, "FAT_" + unique(), "تسمين", "Fattening");

        mockMvc.perform(patch("/api/v1/farms/{farmId}/animal-groups/{groupId}/status", farmId, id)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.nameEn").value("Fattening"));

        mockMvc.perform(patch("/api/v1/farms/{farmId}/animal-groups/{groupId}/status", farmId, id)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void putRejectsCodeChange() throws Exception {
        String farmId = seededFarmId();
        String suffix = unique();
        String id = createGroup(farmId, "KEEP_" + suffix, "ثابت", "Keep");

        mockMvc.perform(put("/api/v1/farms/{farmId}/animal-groups/{groupId}", farmId, id)
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("CHANGED_" + suffix, "ثابت محدث", "Keep updated")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    }

    @Test
    void assignmentRouteIsNotMapped() throws Exception {
        String farmId = seededFarmId();
        String id = createGroup(farmId, "NOASG_" + unique(), "بدون", "None");

        mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups/{groupId}/animals", farmId, id)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"animalIds\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor manageAndView() {
        return JwtAuth.withPermissions(Permission.GROUP_MANAGE.name(), Permission.GROUP_VIEW.name());
    }

    private String seededFarmId() {
        return farmRepository
                .findByCodeIgnoreCase("HARRI")
                .orElseThrow()
                .getId()
                .toString();
    }

    private String otherFarmId() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO farm (id, code, name_ar, name_en, active, created_at, created_by, updated_at, updated_by)
                VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test')
                """,
                id,
                "OTHER_" + unique(),
                "مزرعة أخرى",
                "Other Farm");
        return id.toString();
    }

    private String createGroup(String farmId, String code, String nameAr, String nameEn) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/farms/{farmId}/animal-groups", farmId)
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(code, nameAr, nameEn)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private static String createJson(String code, String nameAr, String nameEn) {
        String type = code.startsWith("YOUNG") ? "YOUNG" : code.startsWith("FAT") ? "FATTENING" : "DAMS";
        return """
                {
                  "code": "%s",
                  "nameAr": "%s",
                  "nameEn": "%s",
                  "groupTypeCode": "%s"
                }
                """.formatted(code, nameAr, nameEn, type);
    }

    private static String updateJson(String code, String nameAr, String nameEn) {
        return """
                {
                  "code": "%s",
                  "nameAr": "%s",
                  "nameEn": "%s",
                  "groupTypeCode": "ISOLATION"
                }
                """.formatted(code, nameAr, nameEn);
    }

    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
