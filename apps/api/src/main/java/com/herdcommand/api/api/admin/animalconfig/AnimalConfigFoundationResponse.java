package com.herdcommand.api.api.admin.animalconfig;

import java.util.List;

public record AnimalConfigFoundationResponse(
        String defaultLocale,
        List<String> supportedLocales,
        List<String> permissions
) {}
