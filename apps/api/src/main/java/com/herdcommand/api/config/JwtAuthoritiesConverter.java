package com.herdcommand.api.config;

import com.herdcommand.api.security.JwtRoleExtractor;
import com.herdcommand.api.security.Permission;
import com.herdcommand.api.security.RolePermissionMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        Collection<GrantedAuthority> scoped = scopes.convert(jwt);
        if (scoped != null) {
            authorities.addAll(scoped);
        }

        List<String> tokenRoles = JwtRoleExtractor.memberships(jwt);
        tokenRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);

        Set<Permission> permissions = new LinkedHashSet<>(RolePermissionMapper.permissionsForRoles(tokenRoles));
        permissions.addAll(explicitPermissions(jwt, tokenRoles));
        permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .forEach(authorities::add);
        return authorities;
    }

    private Set<Permission> explicitPermissions(Jwt jwt, List<String> tokenRoles) {
        Set<Permission> permissions = new LinkedHashSet<>();
        for (String value : stringList(jwt.getClaim("permissions"))) {
            parsePermission(value).ifPresent(permissions::add);
        }
        for (String value : tokenRoles) {
            parsePermission(value).ifPresent(permissions::add);
        }
        return permissions;
    }

    private static java.util.Optional<Permission> parsePermission(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Permission.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
