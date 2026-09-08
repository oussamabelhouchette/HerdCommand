package com.herdcommand.api.domain.farm;

import java.util.Locale;

public enum FarmLanguage {
    AR("ar"),
    FR("fr");

    private final String code;

    FarmLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static FarmLanguage fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("defaultLanguage");
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (FarmLanguage value : values()) {
            if (value.code.equals(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException(raw);
    }
}
