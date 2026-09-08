package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.error.ApiError;
import com.herdcommand.api.api.error.BadRequestException;
import com.herdcommand.api.api.error.ErrorCodes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public final class FarmPlanLimits {

    public static final int TRIAL_MAX_ANIMALS = 50;
    public static final int TRIAL_MAX_TEAM = 3;
    public static final int TRIAL_DAYS = 30;
    public static final int TRIAL_MAX_DAYS = 365;

    public static final int ESSENTIAL_MAX_ANIMALS = 300;
    public static final int ESSENTIAL_MAX_TEAM = 10;

    public static final int PROFESSIONAL_MAX_ANIMALS = 2000;
    public static final int PROFESSIONAL_MAX_TEAM = 50;

    private FarmPlanLimits() {}

    public record Applied(
            FarmPlanCode planCode,
            int maxActiveAnimals,
            int maxTeamMembers,
            Instant trialEndsAt
    ) {}

    public static FarmPlanCode requirePlan(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(ErrorCodes.PLAN_NOT_FOUND, "error.plan.notFound");
        }
        try {
            return FarmPlanCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCodes.PLAN_NOT_FOUND, "error.plan.notFound");
        }
    }

    public static Applied resolve(
            FarmPlanCode plan,
            Integer requestedAnimals,
            Integer requestedTeam,
            LocalDate trialEndsAt,
            ZoneId zone) {
        int capAnimals = maxAnimals(plan);
        int capTeam = maxTeam(plan);
        rejectIfOver("subscription.maxActiveAnimals", requestedAnimals, capAnimals);
        rejectIfOver("subscription.maxTeamMembers", requestedTeam, capTeam);

        Instant trial = null;
        if (plan == FarmPlanCode.TRIAL) {
            Instant now = Instant.now();
            Instant defaultEnd = now.plus(TRIAL_DAYS, ChronoUnit.DAYS);
            Instant latest = now.plus(TRIAL_MAX_DAYS, ChronoUnit.DAYS);
            if (trialEndsAt == null) {
                trial = defaultEnd;
            } else {
                Instant requested = trialEndsAt.atStartOfDay(zone).toInstant();
                if (!requested.isAfter(now) || requested.isAfter(latest)) {
                    throw invalidLimit("subscription.trialEndsAt");
                }
                trial = requested;
            }
        } else if (trialEndsAt != null) {
            throw invalidLimit("subscription.trialEndsAt");
        }

        return new Applied(plan, capAnimals, capTeam, trial);
    }

    static int maxAnimals(FarmPlanCode plan) {
        return switch (plan) {
            case TRIAL -> TRIAL_MAX_ANIMALS;
            case ESSENTIAL -> ESSENTIAL_MAX_ANIMALS;
            case PROFESSIONAL -> PROFESSIONAL_MAX_ANIMALS;
        };
    }

    static int maxTeam(FarmPlanCode plan) {
        return switch (plan) {
            case TRIAL -> TRIAL_MAX_TEAM;
            case ESSENTIAL -> ESSENTIAL_MAX_TEAM;
            case PROFESSIONAL -> PROFESSIONAL_MAX_TEAM;
        };
    }

    private static void rejectIfOver(String field, Integer requested, int cap) {
        if (requested == null) {
            return;
        }
        if (requested < 1 || requested > cap) {
            throw invalidLimit(field);
        }
    }

    private static BadRequestException invalidLimit(String field) {
        return new BadRequestException(
                ErrorCodes.PLAN_LIMIT_INVALID,
                "error.plan.limitInvalid",
                List.of(new ApiError.FieldError(field, "invalid")));
    }
}
