package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.error.ApiException;
import com.herdcommand.api.api.error.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FarmPlanLimitsTest {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    @Test
    void trialUsesServerCapsAndIgnoresMissingClientLimits() {
        FarmPlanLimits.Applied applied = FarmPlanLimits.resolve(
                FarmPlanCode.TRIAL, null, null, null, TUNIS);

        assertThat(applied.maxActiveAnimals()).isEqualTo(FarmPlanLimits.TRIAL_MAX_ANIMALS);
        assertThat(applied.maxTeamMembers()).isEqualTo(FarmPlanLimits.TRIAL_MAX_TEAM);
        assertThat(applied.trialEndsAt()).isNotNull();
    }

    @Test
    void trialRejectsClientLimitsAboveTheCatalog() {
        assertThatThrownBy(() -> FarmPlanLimits.resolve(
                FarmPlanCode.TRIAL, 300, 10, null, TUNIS))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.PLAN_LIMIT_INVALID);
    }

    @Test
    void unknownPlanIsNotFound() {
        assertThatThrownBy(() -> FarmPlanLimits.requirePlan("ENTERPRISE"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.PLAN_NOT_FOUND);
    }

    @Test
    void paidPlanCannotCarryATrialEndDate() {
        assertThatThrownBy(() -> FarmPlanLimits.resolve(
                FarmPlanCode.ESSENTIAL, 100, 5, LocalDate.now().plusDays(10), TUNIS))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCodes.PLAN_LIMIT_INVALID);
    }
}
