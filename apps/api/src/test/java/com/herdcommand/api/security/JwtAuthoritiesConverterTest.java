package com.herdcommand.api.security;

import com.herdcommand.api.config.JwtAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthoritiesConverterTest {

    private final JwtAuthoritiesConverter converter = new JwtAuthoritiesConverter();

    @Test
    void managerRoleReceivesConfigurationPermissions() {
        Jwt jwt = jwtWithRealmRoles("manager");

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities)
                .contains("ROLE_manager")
                .contains(Permission.ANIMAL_CONFIG_VIEW.name())
                .contains(Permission.BREED_MANAGE.name())
                .contains(Permission.STATUS_CONFIG_MANAGE.name())
                .contains(Permission.GROUP_VIEW.name())
                .contains(Permission.GROUP_MANAGE.name())
                .doesNotContain(Permission.PLATFORM_ADMIN.name());
    }

    @Test
    void ownerDoesNotReceivePlatformAdmin() {
        Jwt jwt = jwtWithRealmRoles("owner");

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities)
                .contains(Permission.ANIMAL_CONFIG_VIEW.name())
                .doesNotContain(Permission.PLATFORM_ADMIN.name());
    }

    @Test
    void platformAdminRealmRoleReceivesPlatformAdminPermission() {
        Jwt jwt = jwtWithRealmRoles("PLATFORM_ADMIN");

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities)
                .contains("ROLE_PLATFORM_ADMIN")
                .contains(Permission.PLATFORM_ADMIN.name())
                .doesNotContain(Permission.BREED_MANAGE.name());
    }

    @Test
    void platformAdminClientRoleReceivesPlatformAdminPermission() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("resource_access", Map.of(
                        "herdcommand", Map.of("roles", List.of("platform_admin"))))
                .build();

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).contains(Permission.PLATFORM_ADMIN.name());
    }

    @Test
    void explicitPermissionClaimIsGranted() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("permissions", List.of("GROUP_VIEW"))
                .build();

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).contains(Permission.GROUP_VIEW.name());
        assertThat(authorities).doesNotContain(Permission.BREED_MANAGE.name());
    }

    private static Jwt jwtWithRealmRoles(String... roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
    }
}
