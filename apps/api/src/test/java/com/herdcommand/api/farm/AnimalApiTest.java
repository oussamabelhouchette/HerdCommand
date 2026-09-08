package com.herdcommand.api.farm;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.breed.AnimalBreed;
import com.herdcommand.api.domain.breed.AnimalBreedRepository;
import com.herdcommand.api.domain.breed.SpeciesCode;
import com.herdcommand.api.domain.farm.AssignFarmOwnerCommand;
import com.herdcommand.api.domain.farm.CreateFarmCommand;
import com.herdcommand.api.domain.farm.FarmTenantService;
import com.herdcommand.api.domain.farm.FarmTenantSnapshot;
import com.herdcommand.api.domain.group.AnimalGroup;
import com.herdcommand.api.domain.group.AnimalGroupRepository;
import com.herdcommand.api.domain.group.GroupTypeCode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
@Transactional
class AnimalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmTenantService farmTenantService;

    @Autowired
    private AnimalBreedRepository breedRepository;

    @Autowired
    private AnimalGroupRepository groupRepository;

    private FarmTenantSnapshot farmA;
    private FarmTenantSnapshot farmB;
    private AnimalBreed najdi;
    private AnimalGroup groupA;
    private AnimalGroup groupB;

    @BeforeEach
    void setUp() {
        authenticateAuditor();
        farmA = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة أ", "Farm A", "Ferme A", "TN-11", null, null, "ar"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farmA.id(), "kc-owner-a", "ownera@example.tn", "Owner A"));
        farmB = farmTenantService.createFarm(new CreateFarmCommand(
                "مزرعة ب", "Farm B", "Ferme B", "TN-12", null, null, "ar"));
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farmB.id(), "kc-owner-b", "ownerb@example.tn", "Owner B"));
        najdi = breedRepository.saveAndFlush(new AnimalBreed("NAJDI-TEST", "نجدي", "Najdi", SpeciesCode.SHEEP, 1));
        groupA = groupRepository.saveAndFlush(new AnimalGroup(
                farmA.id(), "DAMS-A", "أمهات أ", "Dams A", GroupTypeCode.DAMS, null, null));
        groupB = groupRepository.saveAndFlush(new AnimalGroup(
                farmB.id(), "DAMS-B", "أمهات ب", "Dams B", GroupTypeCode.DAMS, null, null));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthorizedFarmIsForbiddenWithoutLeakingRecords() throws Exception {
        createAnimal(farmA.id(), "kc-owner-a", "OVL-1", "رغد", groupA.getId());

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .with(owner("kc-owner-b"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", UUID.randomUUID())
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.FORBIDDEN));
    }

    @Test
    void listIsIsolatedByFarmAndPageSizeIsTwenty() throws Exception {
        for (int i = 1; i <= 21; i++) {
            createAnimal(farmA.id(), "kc-owner-a", "A-" + i, "حيوان " + i, groupA.getId());
        }
        createAnimal(farmB.id(), "kc-owner-b", "B-1", "من ب", groupB.getId());

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .param("page", "0")
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "ar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.items", hasSize(20)));

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .param("page", "1")
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void searchAndFiltersUseTheFullFarmDataset() throws Exception {
        createAnimal(farmA.id(), "kc-owner-a", "OVL-2101", "رغد", groupA.getId());
        createAnimal(farmA.id(), "kc-owner-a", "OVL-2102", "صخر", groupA.getId());

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .param("search", "رغد")
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].identificationNumber").value("OVL-2101"));

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .param("search", "ovl-2102")
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("صخر"));

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .param("groupId", groupA.getId().toString())
                        .param("statusCode", "ACTIVE")
                        .param("gender", "FEMALE")
                        .param("breedId", najdi.getId().toString())
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("رغد"));
    }

    @Test
    void rejectsGroupFromAnotherFarm() throws Exception {
        mockMvc.perform(post("/api/v1/farms/{farmId}/animals", farmA.id())
                        .with(owner("kc-owner-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(animalJson("OVL-X", "عابر", groupB.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.GROUP_NOT_IN_FARM));
    }

    @Test
    void createUpdateAndArchiveStayOnTheOwnedFarm() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/farms/{farmId}/animals", farmA.id())
                        .with(owner("kc-owner-a"))
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(animalJson("OVL-2101", "رغد", groupA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationNumber").value("OVL-2101"))
                .andExpect(jsonPath("$.breed.code").value("NAJDI-TEST"))
                .andExpect(jsonPath("$.status.code").value("ACTIVE"))
                .andExpect(jsonPath("$.group.id").value(groupA.getId().toString()))
                .andReturn();

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(put("/api/v1/farms/{farmId}/animals/{animalId}", farmA.id(), id)
                        .with(owner("kc-owner-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(animalJson("OVL-2101", "رغد المحدثة", groupA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("رغد المحدثة"));

        mockMvc.perform(delete("/api/v1/farms/{farmId}/animals/{animalId}", farmA.id(), id)
                        .with(owner("kc-owner-a")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/farms/{farmId}/animals", farmA.id())
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void createAllowsMissingOrBlankGroup() throws Exception {
        mockMvc.perform(post("/api/v1/farms/{farmId}/animals", farmA.id())
                        .with(owner("kc-owner-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(animalJson("OVL-NONE", "بدون مجموعة", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.group").doesNotExist());

        mockMvc.perform(post("/api/v1/farms/{farmId}/animals", farmA.id())
                        .with(owner("kc-owner-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identificationNumber": "OVL-BLANK",
                                  "name": "فارغ",
                                  "breedId": "%s",
                                  "statusCode": "ACTIVE",
                                  "genderCode": "FEMALE",
                                  "groupId": ""
                                }
                                """.formatted(najdi.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationNumber").value("OVL-BLANK"));
    }

    @Test
    void lookupsAreScopedToTheActiveFarm() throws Exception {
        mockMvc.perform(get("/api/v1/farms/{farmId}/animals/lookups", farmA.id())
                        .with(owner("kc-owner-a"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].id").value(groupA.getId().toString()))
                .andExpect(jsonPath("$.genders[0].code").value("FEMALE"))
                .andExpect(jsonPath("$.statuses[0].code").value("ACTIVE"));
    }

    private void createAnimal(UUID farmId, String owner, String identification, String name, UUID groupId)
            throws Exception {
        mockMvc.perform(post("/api/v1/farms/{farmId}/animals", farmId)
                        .with(owner(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(animalJson(identification, name, groupId)))
                .andExpect(status().isCreated());
    }

    private String animalJson(String identification, String name, UUID groupId) {
        return """
                {
                  "identificationNumber": "%s",
                  "name": "%s",
                  "breedId": "%s",
                  "statusCode": "ACTIVE",
                  "genderCode": "%s",
                  "dateOfBirth": "%s"%s
                }
                """.formatted(
                identification,
                name,
                najdi.getId(),
                name.contains("صخر") ? "MALE" : "FEMALE",
                LocalDate.now().minusYears(2),
                groupId == null ? "" : ",\n                  \"groupId\": \"%s\"".formatted(groupId));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor owner(String subject) {
        return JwtAuth.withPermissionsAs(
                subject, Permission.ANIMAL_VIEW.name(), Permission.ANIMAL_MANAGE.name());
    }

    private static void authenticateAuditor() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("platform-admin-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
