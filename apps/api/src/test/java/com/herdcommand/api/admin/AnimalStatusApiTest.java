package com.herdcommand.api.admin;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.status.AnimalStatusService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class AnimalStatusApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnimalStatusService animalStatusService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-statuses").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void listRequiresViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-statuses")
                        .with(JwtAuth.authenticated())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    @Test
    void updateRequiresManagePermission() throws Exception {
        mockMvc.perform(put("/api/v1/admin/animal-statuses/PREGNANT")
                        .with(JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("حامل", "Pregnant", "purple", 20, true, true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void seedCreatesExactlyFourProtectedStatuses() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-statuses")
                        .with(view())
                        .param("size", "20")
                        .param("sort", "displayOrder,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[*].code", hasItems("ACTIVE", "PREGNANT", "SICK", "ISOLATED")))
                .andExpect(jsonPath("$.items[0].code").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].colorToken").value("success"))
                .andExpect(jsonPath("$.items[0].systemProtected").value(true))
                .andExpect(jsonPath("$.items[0].createdBy").value("system"));
    }

    @Test
    void getPregnantReturnsSeededPresentation() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-statuses/{code}", "pregnant")
                        .with(view())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PREGNANT"))
                .andExpect(jsonPath("$.labelEn").value("Pregnant"))
                .andExpect(jsonPath("$.colorToken").value("purple"));
    }

    @Test
    void updateChangesPresentationAndKeepsCode() throws Exception {
        try {
            mockMvc.perform(put("/api/v1/admin/animal-statuses/PREGNANT")
                            .with(manageAndView())
                            .header("Accept-Language", "en")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson("حامل محدث", "Pregnant updated", "neutral", 25, false, true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("PREGNANT"))
                    .andExpect(jsonPath("$.labelAr").value("حامل محدث"))
                    .andExpect(jsonPath("$.labelEn").value("Pregnant updated"))
                    .andExpect(jsonPath("$.colorToken").value("neutral"))
                    .andExpect(jsonPath("$.displayOrder").value(25))
                    .andExpect(jsonPath("$.visibleInFilter").value(false))
                    .andExpect(jsonPath("$.updatedBy").value("user-1"));

            mockMvc.perform(get("/api/v1/admin/animal-statuses/PREGNANT").with(view()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("PREGNANT"))
                    .andExpect(jsonPath("$.labelEn").value("Pregnant updated"));
        } finally {
            restorePregnant();
        }
    }

    @Test
    void rejectedColorTokenReturnsInvalidColor() throws Exception {
        mockMvc.perform(put("/api/v1/admin/animal-statuses/PREGNANT")
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("حامل", "Pregnant", "url(...)", 20, true, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_COLOR_TOKEN))
                .andExpect(jsonPath("$.message").value("This color token is not allowed."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("colorToken"));

        mockMvc.perform(put("/api/v1/admin/animal-statuses/PREGNANT")
                        .with(manageAndView())
                        .header("Accept-Language", "ar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("حامل", "Pregnant", "#ff00aa", 20, true, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_COLOR_TOKEN))
                .andExpect(jsonPath("$.message").value("رمز اللون هذا غير مسموح به."));
    }

    @Test
    void cannotDeactivateActiveStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/animal-statuses/ACTIVE/status")
                        .with(manageAndView())
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.STATUS_REQUIRED));

        mockMvc.perform(put("/api/v1/admin/animal-statuses/ACTIVE")
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("نشط", "Active", "success", 10, true, false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCodes.STATUS_REQUIRED));
    }

    @Test
    void deactivateSickKeepsTheRow() throws Exception {
        try {
            mockMvc.perform(patch("/api/v1/admin/animal-statuses/SICK/status")
                            .with(manageAndView())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.labelEn").value("Sick"))
                    .andExpect(jsonPath("$.code").value("SICK"));

            mockMvc.perform(get("/api/v1/admin/animal-statuses/SICK").with(view()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        } finally {
            restoreSick();
        }
    }

    @Test
    void searchFiltersByTextAndActive() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-statuses")
                        .with(view())
                        .param("search", "Pregnant")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "displayOrder,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].code").value("PREGNANT"));
    }

    @Test
    void missingStatusReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/animal-statuses/{code}", "UNKNOWN")
                        .with(view())
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND));
    }

    @Test
    void cacheEvictsAfterUpdate() throws Exception {
        try {
            assertThat(animalStatusService.listActive())
                    .anyMatch(status -> "PREGNANT".equals(status.code()) && "Pregnant".equals(status.labelEn()));

            mockMvc.perform(put("/api/v1/admin/animal-statuses/PREGNANT")
                            .with(manageAndView())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson("حامل", "CachedPregnant", "purple", 20, true, true)))
                    .andExpect(status().isOk());

            assertThat(animalStatusService.listActive())
                    .anyMatch(status -> "PREGNANT".equals(status.code()) && "CachedPregnant".equals(status.labelEn()));
        } finally {
            restorePregnant();
        }
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor view() {
        return JwtAuth.withPermissions(Permission.ANIMAL_CONFIG_VIEW.name());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor manageAndView() {
        return JwtAuth.withPermissions(Permission.STATUS_CONFIG_MANAGE.name(), Permission.ANIMAL_CONFIG_VIEW.name());
    }

    private void restorePregnant() throws Exception {
        mockMvc.perform(put("/api/v1/admin/animal-statuses/PREGNANT")
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("حامل", "Pregnant", "purple", 20, true, true)))
                .andExpect(status().isOk());
    }

    private void restoreSick() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/animal-statuses/SICK/status")
                        .with(manageAndView())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
    }

    private static String updateJson(
            String labelAr, String labelEn, String colorToken, int displayOrder, boolean visible, boolean active) {
        return """
                {
                  "labelAr": "%s",
                  "labelEn": "%s",
                  "colorToken": "%s",
                  "displayOrder": %d,
                  "visibleInFilter": %s,
                  "active": %s
                }
                """.formatted(labelAr, labelEn, colorToken, displayOrder, visible, active);
    }
}
