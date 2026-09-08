package com.herdcommand.api.platform;

import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.domain.farm.AssignFarmOwnerCommand;
import com.herdcommand.api.domain.farm.CreateFarmCommand;
import com.herdcommand.api.domain.farm.Farm;
import com.herdcommand.api.domain.farm.FarmCodeGenerator;
import com.herdcommand.api.domain.farm.FarmMembership;
import com.herdcommand.api.domain.farm.FarmMembershipRepository;
import com.herdcommand.api.domain.farm.FarmMembershipRole;
import com.herdcommand.api.domain.farm.FarmMembershipStatus;
import com.herdcommand.api.domain.farm.FarmPlanCode;
import com.herdcommand.api.domain.farm.FarmRepository;
import com.herdcommand.api.domain.farm.FarmStatus;
import com.herdcommand.api.domain.farm.FarmSubscriptionRepository;
import com.herdcommand.api.domain.farm.FarmTenantService;
import com.herdcommand.api.domain.farm.FarmTenantSnapshot;
import com.herdcommand.api.domain.farm.UpdateFarmIdentityCommand;
import com.herdcommand.api.support.TestJwtConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class FarmTenantFoundationTest {

    @Autowired
    private FarmTenantService farmTenantService;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmMembershipRepository membershipRepository;

    @Autowired
    private FarmSubscriptionRepository subscriptionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void authenticate() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("platform-admin-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistedFarmReceivesUniqueImmutableServerCode() {
        FarmTenantSnapshot first = createDraft("مزرعة أ", "Farm A", "Ferme A");
        FarmTenantSnapshot second = createDraft("مزرعة ب", "Farm B", "Ferme B");

        assertThat(first.code()).matches("^" + FarmCodeGenerator.PREFIX + "\\d{4}$");
        assertThat(second.code()).matches("^" + FarmCodeGenerator.PREFIX + "\\d{4}$");
        assertThat(first.code()).isNotEqualTo(second.code());
        assertThat(first.status()).isEqualTo(FarmStatus.SETUP);
        assertThat(first.active()).isFalse();
        assertThat(first.currencyCode()).isEqualTo(Farm.CURRENCY_TND);
        assertThat(first.timezone()).isEqualTo(Farm.DEFAULT_TIMEZONE);

        Farm stored = farmRepository.findById(first.id()).orElseThrow();
        assertThat(stored.getCode()).isEqualTo(first.code());
        assertThat(stored.getCreatedBy()).isEqualTo("platform-admin-1");
        assertThat(subscriptionRepository.findByFarmId(first.id()).orElseThrow().getPlanCode())
                .isEqualTo(FarmPlanCode.TRIAL);
    }

    @Test
    void farmCodeIsUniqueAtTheDatabase() {
        FarmTenantSnapshot created = createDraft("مزرعة ج", "Farm C", "Ferme C");
        UUID duplicateId = UUID.randomUUID();
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO farm (
                    id, code, name_ar, name_en, name_fr, governorate_code, timezone,
                    default_language, currency_code, status, version, active,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?, 'TN-11', 'Africa/Tunis', 'ar', 'TND', 'SETUP', 0, FALSE,
                    CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test')
                """,
                duplicateId,
                created.code(),
                "مكررة",
                "Duplicate",
                "Doublon"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void activationWithoutActiveOwnerFails() {
        FarmTenantSnapshot farm = createDraft("بدون مالك", "No Owner", "Sans proprietaire");

        assertThatThrownBy(() -> farmTenantService.activate(farm.id()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.FARM_OWNER_REQUIRED);

        assertThat(farmRepository.findById(farm.id()).orElseThrow().getStatus())
                .isEqualTo(FarmStatus.SETUP);
    }

    @Test
    void activationSucceedsWhenAnActiveOwnerExists() {
        FarmTenantSnapshot farm = createDraft("مع مالك", "With Owner", "Avec proprietaire");
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farm.id(), "kc-owner-1", "Owner@Example.com"));

        FarmMembership membership = membershipRepository
                .findByFarmIdAndKeycloakUserId(farm.id(), "kc-owner-1")
                .orElseThrow();
        assertThat(membership.getInvitedEmail()).isEqualTo("owner@example.com");
        assertThat(membership.getRoleCode()).isEqualTo(FarmMembershipRole.FARM_OWNER);
        assertThat(membership.getStatus()).isEqualTo(FarmMembershipStatus.ACTIVE);
        assertThat(membership.getAcceptedAt()).isNotNull();

        FarmTenantSnapshot active = farmTenantService.activate(farm.id());
        assertThat(active.status()).isEqualTo(FarmStatus.ACTIVE);
        assertThat(active.active()).isTrue();
    }

    @Test
    void cannotRemoveLastOwnerFromAnActiveFarm() {
        FarmTenantSnapshot farm = createDraft("مالك وحيد", "Only Owner", "Seul proprietaire");
        FarmMembership owner = farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farm.id(), "kc-only-owner", "only@example.com"));
        farmTenantService.activate(farm.id());

        assertThatThrownBy(() -> farmTenantService.removeOwner(farm.id(), owner.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.FARM_OWNER_REQUIRED);
    }

    @Test
    void duplicateActiveMembershipIsRejected() {
        FarmTenantSnapshot farm = createDraft("عضوية", "Membership", "Adhesion");
        farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farm.id(), "kc-dup", "dup@example.com"));

        assertThatThrownBy(() -> farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farm.id(), "kc-dup", "dup2@example.com")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.OWNER_ALREADY_ASSIGNED);

        FarmMembership extra = new FarmMembership(
                farm.id(),
                "kc-dup",
                "other@example.com",
                Instant.now(),
                "platform-admin-1",
                FarmMembershipStatus.INVITED);
        assertThatThrownBy(() -> membershipRepository.saveAndFlush(extra))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void membershipAndSubscriptionQueriesAreFarmScoped() {
        FarmTenantSnapshot farmA = createDraft("أ", "A", "A");
        FarmTenantSnapshot farmB = createDraft("ب", "B", "B");
        FarmMembership owner = farmTenantService.assignOwner(new AssignFarmOwnerCommand(
                farmA.id(), "kc-scoped", "scoped@example.com"));

        assertThat(membershipRepository.findByIdAndFarmId(owner.getId(), farmA.id())).isPresent();
        assertThat(membershipRepository.findByIdAndFarmId(owner.getId(), farmB.id())).isEmpty();
        assertThat(subscriptionRepository.findByFarmId(farmA.id())).isPresent();
        assertThat(subscriptionRepository.findByIdAndFarmId(
                subscriptionRepository.findByFarmId(farmA.id()).orElseThrow().getId(),
                farmB.id()))
                .isEmpty();
    }

    @Test
    void staleVersionIsRejected() {
        FarmTenantSnapshot farm = createDraft("إصدار", "Version", "Version");
        farmTenantService.updateIdentity(identity(farm, farm.version(), "اسم جديد"));

        assertThatThrownBy(() -> farmTenantService.updateIdentity(identity(farm, farm.version(), "قديم")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.FARM_VERSION_CONFLICT);
    }

    @Test
    void concurrentJpaUpdateRaisesOptimisticLock() {
        FarmTenantSnapshot created = createDraft("تزامن", "Concurrent", "Concurrent");
        Farm first = farmRepository.findById(created.id()).orElseThrow();
        entityManager.detach(first);

        Farm second = farmRepository.findById(created.id()).orElseThrow();
        second.setNameAr("تحديث أول");
        farmRepository.saveAndFlush(second);

        first.setNameAr("تحديث قديم");
        assertThatThrownBy(() -> farmRepository.saveAndFlush(first))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void seededHarriFarmKeepsAnimalGroupCompatibility() {
        Farm harri = farmRepository.findByCodeIgnoreCase("HARRI").orElseThrow();
        assertThat(harri.getStatus()).isEqualTo(FarmStatus.ACTIVE);
        assertThat(harri.isActive()).isTrue();
        assertThat(harri.getNameFr()).isEqualTo(harri.getNameEn());
        assertThat(harri.getTimezone()).isEqualTo(Farm.DEFAULT_TIMEZONE);
        assertThat(subscriptionRepository.findByFarmId(harri.getId())).isPresent();
    }

    private FarmTenantSnapshot createDraft(String nameAr, String nameEn, String nameFr) {
        return farmTenantService.createFarm(new CreateFarmCommand(
                nameAr, nameEn, nameFr, "TN-11", null, null, "ar"));
    }

    private static UpdateFarmIdentityCommand identity(FarmTenantSnapshot farm, long version, String nameAr) {
        return new UpdateFarmIdentityCommand(
                farm.id(),
                version,
                nameAr,
                farm.nameEn(),
                farm.nameFr(),
                farm.governorateCode(),
                farm.address(),
                farm.timezone(),
                farm.defaultLanguage());
    }
}
