package com.herdcommand.api.domain.identity;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddresses {

    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private EmailAddresses() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        return email != null && !email.isBlank() && EMAIL.matcher(email).matches();
    }

    public static String requireNormalized(String raw) {
        String email = normalize(raw);
        if (!isValid(email)) {
            throw new IllegalArgumentException("email");
        }
        return email;
    }
}
