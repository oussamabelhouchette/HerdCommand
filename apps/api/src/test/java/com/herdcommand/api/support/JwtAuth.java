package com.herdcommand.api.support;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class JwtAuth {

    private JwtAuth() {}

    public static RequestPostProcessor authenticated() {
        return jwt().jwt(token -> token.subject("user-1"));
    }

    public static RequestPostProcessor withPermissions(String... permissions) {
        GrantedAuthority[] authorities = Arrays.stream(permissions)
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new);
        return jwt().authorities(authorities).jwt(token -> token.subject("user-1"));
    }
}
