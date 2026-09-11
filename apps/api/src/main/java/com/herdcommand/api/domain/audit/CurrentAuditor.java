package com.herdcommand.api.domain.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Resolves the JPA audit actor from the current JWT.
 * Keycloak 26 access tokens omit {@code sub} unless the client has the {@code basic} scope;
 * {@code preferred_username} is then used so {@code created_by} is never null.
 */
public final class CurrentAuditor {

    private static final int MAX_LENGTH = 64;

    private CurrentAuditor() {}

    public static Optional<String> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String fromJwt = firstNonBlank(
                    jwt.getSubject(),
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getClaimAsString("email"));
            if (fromJwt != null) {
                return Optional.of(clip(fromJwt));
            }
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return Optional.empty();
        }
        return Optional.of(clip(name));
    }

    public static String orSystem() {
        return find().orElse("system");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String clip(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
