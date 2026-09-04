package com.herdcommand.api.api.me;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Identity")
public class MeController {

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated identity from the access token")
    @SecurityRequirement(name = "bearer-jwt")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        String username = firstNonBlank(jwt.getClaimAsString("preferred_username"), jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        return new MeResponse(jwt.getSubject(), username, email, extractRoles(jwt));
    }

    private List<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        }
        return List.of();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
