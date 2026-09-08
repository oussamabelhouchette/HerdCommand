package com.herdcommand.api.identity.keycloak;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakAdminPropertiesTest {

    @Test
    void stringFormDoesNotIncludeTheClientSecret() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setClientSecret("super-secret-value");

        assertThat(properties.toString()).doesNotContain("super-secret-value");
        assertThat(properties.toString()).contains("secretConfigured=true");
    }

    @Test
    void keycloakClientSourceDoesNotLogTokensOrSecrets() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/herdcommand/api/identity/keycloak/KeycloakIdentityDirectory.java"));
        assertThat(source).doesNotContain("log.info(properties.getClientSecret");
        assertThat(source).doesNotContain("log.info(form");
        assertThat(source).doesNotContain("log.info(response");
        assertThat(source).contains("TokenValue[redacted]");
        assertThat(source).contains("TokenResponse[redacted]");
    }
}
