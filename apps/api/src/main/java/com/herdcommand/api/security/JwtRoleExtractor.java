package com.herdcommand.api.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads Keycloak realm and client roles from a JWT. Platform admin may be assigned
 * as either a realm role or a client role named {@code PLATFORM_ADMIN}.
 */
public final class JwtRoleExtractor {

    private JwtRoleExtractor() {}

    public static List<String> realmAndClientRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        roles.addAll(realmRoles(jwt));
        roles.addAll(clientRoles(jwt));
        return List.copyOf(roles);
    }

    public static List<String> realmRoles(Jwt jwt) {
        return stringList(claimMap(jwt.getClaim("realm_access")).get("roles"));
    }

    public static List<String> clientRoles(Jwt jwt) {
        Object resourceAccess = jwt.getClaim("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> clients)) {
            return List.of();
        }
        List<String> roles = new ArrayList<>();
        for (Object client : clients.values()) {
            roles.addAll(stringList(claimMap(client).get("roles")));
        }
        return List.copyOf(roles);
    }

    private static Map<?, ?> claimMap(Object claim) {
        if (claim instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
