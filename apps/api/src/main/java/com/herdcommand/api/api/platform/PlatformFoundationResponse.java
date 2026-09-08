package com.herdcommand.api.api.platform;

import java.util.List;

public record PlatformFoundationResponse(
        String defaultLocale,
        List<String> supportedLocales,
        List<String> permissions
) {}
