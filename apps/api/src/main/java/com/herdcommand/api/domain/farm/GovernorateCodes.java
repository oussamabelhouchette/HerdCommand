package com.herdcommand.api.domain.farm;

import java.util.Locale;
import java.util.Set;

public final class GovernorateCodes {

    public static final Set<String> ALL = Set.of(
            "TN-11", "TN-12", "TN-13", "TN-14",
            "TN-21", "TN-22", "TN-23",
            "TN-31", "TN-32", "TN-33", "TN-34",
            "TN-41", "TN-42", "TN-43",
            "TN-51", "TN-52", "TN-53",
            "TN-61",
            "TN-71", "TN-72", "TN-73",
            "TN-81", "TN-82", "TN-83");

    private GovernorateCodes() {}

    public static String requireKnown(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("governorateCode");
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALL.contains(code)) {
            throw new IllegalArgumentException(raw);
        }
        return code;
    }

    public static boolean isKnown(String raw) {
        return raw != null && ALL.contains(raw.trim().toUpperCase(Locale.ROOT));
    }
}
