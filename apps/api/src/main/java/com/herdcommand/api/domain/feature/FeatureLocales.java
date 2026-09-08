package com.herdcommand.api.domain.feature;

import java.util.Locale;

public final class FeatureLocales {

    private FeatureLocales() {}

    public static String fromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "ar";
        }
        String first = acceptLanguage.split(",")[0].trim();
        int quality = first.indexOf(';');
        if (quality >= 0) {
            first = first.substring(0, quality).trim();
        }
        String language = first.split("[-_]")[0].toLowerCase(Locale.ROOT);
        return switch (language) {
            case "fr" -> "fr";
            case "en" -> "en";
            default -> "ar";
        };
    }
}
