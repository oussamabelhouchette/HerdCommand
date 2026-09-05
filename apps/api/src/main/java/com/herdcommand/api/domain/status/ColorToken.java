package com.herdcommand.api.domain.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ColorToken {
    SUCCESS("success"),
    PURPLE("purple"),
    DANGER("danger"),
    WARNING("warning"),
    NEUTRAL("neutral");

    private final String token;

    ColorToken(String token) {
        this.token = token;
    }

    @JsonValue
    public String token() {
        return token;
    }

    @JsonCreator
    public static ColorToken fromToken(String raw) {
        ColorToken parsed = tryParse(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(raw);
        }
        return parsed;
    }

    public static ColorToken tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (ColorToken value : values()) {
            if (value.token.equals(key)) {
                return value;
            }
        }
        return null;
    }
}
