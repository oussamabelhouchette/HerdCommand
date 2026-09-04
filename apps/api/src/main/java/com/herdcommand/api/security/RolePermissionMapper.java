package com.herdcommand.api.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RolePermissionMapper {

    private static final Map<String, Set<Permission>> MATRIX = Map.of(
            "owner", EnumSet.allOf(Permission.class),
            "administrator", EnumSet.allOf(Permission.class),
            "manager", EnumSet.allOf(Permission.class),
            "veterinarian", EnumSet.of(Permission.ANIMAL_CONFIG_VIEW, Permission.GROUP_VIEW),
            "worker", EnumSet.of(Permission.ANIMAL_CONFIG_VIEW, Permission.GROUP_VIEW),
            "accountant", EnumSet.of(Permission.ANIMAL_CONFIG_VIEW));

    private RolePermissionMapper() {}

    public static Set<Permission> permissionsForRole(String role) {
        if (role == null || role.isBlank()) {
            return Set.of();
        }
        return MATRIX.getOrDefault(role.trim().toLowerCase(Locale.ROOT), Set.of());
    }

    public static Set<Permission> permissionsForRoles(Iterable<String> roles) {
        EnumSet<Permission> granted = EnumSet.noneOf(Permission.class);
        for (String role : roles) {
            granted.addAll(permissionsForRole(role));
        }
        return Collections.unmodifiableSet(granted);
    }
}
