package com.herdcommand.api.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RolePermissionMapper {

    private static final Set<Permission> ANIMAL_CONFIGURATION = Set.copyOf(EnumSet.of(
            Permission.ANIMAL_CONFIG_VIEW,
            Permission.BREED_MANAGE,
            Permission.STATUS_CONFIG_MANAGE,
            Permission.GROUP_VIEW,
            Permission.GROUP_MANAGE));

    private static final Map<String, Set<Permission>> MATRIX = Map.of(
            "owner", ANIMAL_CONFIGURATION,
            "administrator", ANIMAL_CONFIGURATION,
            "manager", ANIMAL_CONFIGURATION,
            "veterinarian", EnumSet.of(Permission.ANIMAL_CONFIG_VIEW, Permission.GROUP_VIEW),
            "worker", EnumSet.of(Permission.ANIMAL_CONFIG_VIEW, Permission.GROUP_VIEW),
            "accountant", EnumSet.of(Permission.ANIMAL_CONFIG_VIEW),
            "platform_admin", EnumSet.of(Permission.PLATFORM_ADMIN),
            "farm_owner", EnumSet.of(
                    Permission.ANIMAL_VIEW,
                    Permission.ANIMAL_MANAGE,
                    Permission.GROUP_VIEW,
                    Permission.GROUP_MANAGE,
                    Permission.ANIMAL_CONFIG_VIEW));

    private RolePermissionMapper() {}

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    public static Set<Permission> permissionsForRole(String role) {
        if (role == null || role.isBlank()) {
            return Set.of();
        }
        return MATRIX.getOrDefault(normalizeRole(role), Set.of());
    }

    public static Set<Permission> permissionsForRoles(Iterable<String> roles) {
        EnumSet<Permission> granted = EnumSet.noneOf(Permission.class);
        for (String role : roles) {
            granted.addAll(permissionsForRole(role));
        }
        return Collections.unmodifiableSet(granted);
    }
}
