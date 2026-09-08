package com.herdcommand.api.domain.farm;

import com.herdcommand.api.api.platform.farm.PlatformFarmCreatedResponse;

import java.util.UUID;

public sealed interface OnboardingClaim {

    record Proceed(UUID requestId) implements OnboardingClaim {}

    record Replay(PlatformFarmCreatedResponse response) implements OnboardingClaim {}

    record InProgress() implements OnboardingClaim {}
}
