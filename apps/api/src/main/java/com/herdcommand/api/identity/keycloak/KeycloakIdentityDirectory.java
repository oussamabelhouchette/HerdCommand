package com.herdcommand.api.identity.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.herdcommand.api.domain.identity.IdentityDirectory;
import com.herdcommand.api.domain.identity.IdentityProviderException;
import com.herdcommand.api.domain.identity.IdentityRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KeycloakIdentityDirectory implements IdentityDirectory {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityDirectory.class);
    private static final List<String> REQUIRED_ACTIONS = List.of("VERIFY_EMAIL", "UPDATE_PASSWORD");

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;
    private final AtomicReference<CachedToken> token = new AtomicReference<>();

    public KeycloakIdentityDirectory(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(trimSlash(properties.getBaseUrl())).build();
    }

    @Override
    public List<IdentityRecord> findByExactEmail(String normalizedEmail) {
        TokenValue access = token();
        try {
            KeycloakUser[] users = restClient.get()
                    .uri("/admin/realms/{realm}/users?email={email}&exact=true",
                            properties.getRealm(), normalizedEmail)
                    .header(HttpHeaders.AUTHORIZATION, bearer(access))
                    .retrieve()
                    .body(KeycloakUser[].class);
            List<IdentityRecord> records = new ArrayList<>();
            if (users != null) {
                for (KeycloakUser user : users) {
                    records.add(toRecord(user, normalizedEmail));
                }
            }
            log.info("Keycloak exact email search returned {} user(s)", records.size());
            return records;
        } catch (RestClientException ex) {
            throw wrap(ex, "search");
        }
    }

    @Override
    public IdentityRecord createInvitedUser(String normalizedEmail, String displayName) {
        TokenValue access = token();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", normalizedEmail);
        body.put("email", normalizedEmail);
        body.put("enabled", true);
        body.put("emailVerified", false);
        body.put("requiredActions", REQUIRED_ACTIONS);
        applyDisplayName(body, displayName);

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.getRealm())
                    .header(HttpHeaders.AUTHORIZATION, bearer(access))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            String userId = userIdFromLocation(response.getHeaders().getLocation());
            log.info("Created Keycloak invitation user");
            return new IdentityRecord(userId, normalizedEmail, displayName(displayName, normalizedEmail), true);
        } catch (RestClientException ex) {
            throw wrap(ex, "create");
        }
    }

    @Override
    public void sendExecuteActionsEmail(String keycloakUserId) {
        TokenValue access = token();
        try {
            restClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users/{userId}/execute-actions-email")
                            .queryParam("client_id", properties.getPublicClientId())
                            .queryParam("redirect_uri", properties.getLoginRedirectUri())
                            .queryParam("lifespan", properties.getActionLifespanSeconds())
                            .build(properties.getRealm(), keycloakUserId))
                    .header(HttpHeaders.AUTHORIZATION, bearer(access))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(REQUIRED_ACTIONS)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Requested Keycloak execute-actions email");
        } catch (RestClientException ex) {
            throw wrap(ex, "execute-actions-email");
        }
    }

    @Override
    public void deleteCreatedIdentity(String keycloakUserId) {
        TokenValue access = token();
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{userId}", properties.getRealm(), keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(access))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted unused Keycloak identity created by this operation");
        } catch (RestClientException ex) {
            throw wrap(ex, "delete");
        }
    }

    private TokenValue token() {
        if (!properties.hasSecret()) {
            throw new IdentityProviderException("Keycloak admin client secret is not configured");
        }
        CachedToken cached = token.get();
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(5))) {
            return cached.value();
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        try {
            TokenResponse response = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IdentityProviderException("Keycloak token response was empty");
            }
            int expires = response.expiresIn() == null || response.expiresIn() < 30 ? 30 : response.expiresIn();
            TokenValue value = new TokenValue(response.accessToken());
            token.set(new CachedToken(value, Instant.now().plusSeconds(expires)));
            log.info("Obtained Keycloak admin access token");
            return value;
        } catch (RestClientException ex) {
            throw wrap(ex, "token");
        }
    }

    private static String bearer(TokenValue access) {
        return "Bearer " + access.token();
    }

    private static IdentityProviderException wrap(RestClientException ex, String action) {
        if (ex instanceof RestClientResponseException response) {
            log.warn("Keycloak admin {} failed status={}", action, response.getStatusCode().value());
        } else {
            log.warn("Keycloak admin {} failed", action);
        }
        return new IdentityProviderException("Keycloak admin " + action + " failed", ex);
    }

    private static String userIdFromLocation(URI location) {
        if (location == null) {
            throw new IdentityProviderException("Keycloak create user returned no Location");
        }
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash == path.length() - 1) {
            throw new IdentityProviderException("Keycloak create user Location was unreadable");
        }
        return path.substring(slash + 1);
    }

    private static IdentityRecord toRecord(KeycloakUser user, String fallbackEmail) {
        String email = user.email() == null || user.email().isBlank() ? fallbackEmail : user.email();
        return new IdentityRecord(
                user.id(),
                email,
                displayName(joinName(user.firstName(), user.lastName()), user.username() == null ? email : user.username()),
                user.enabled() == null || user.enabled());
    }

    private static void applyDisplayName(Map<String, Object> body, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        String trimmed = displayName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            body.put("firstName", trimmed);
            return;
        }
        body.put("firstName", trimmed.substring(0, space));
        body.put("lastName", trimmed.substring(space + 1).trim());
    }

    private static String joinName(String first, String last) {
        String combined = ((first == null ? "" : first.trim()) + " " + (last == null ? "" : last.trim())).trim();
        return combined.isBlank() ? null : combined;
    }

    private static String displayName(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback;
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:9090";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record TokenValue(String token) {
        @Override
        public String toString() {
            return "TokenValue[redacted]";
        }
    }

    private record CachedToken(TokenValue value, Instant expiresAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
            @com.fasterxml.jackson.annotation.JsonProperty("expires_in") Integer expiresIn
    ) {
        @Override
        public String toString() {
            return "TokenResponse[redacted]";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeycloakUser(String id, String username, String email, String firstName, String lastName, Boolean enabled) {}
}
