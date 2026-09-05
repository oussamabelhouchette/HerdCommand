package com.herdcommand.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDotenvTest {

    @Test
    void parseIgnoresCommentsAndStripsQuotes() {
        Map<String, String> values = LocalDotenv.parse("""
                # comment
                KEYCLOAK_ADMIN_CLIENT_ID=herdcommand-admin
                KEYCLOAK_ADMIN_CLIENT_SECRET="abc def"
                export DATABASE_USERNAME=postgres
                """);

        assertThat(values)
                .containsEntry("KEYCLOAK_ADMIN_CLIENT_ID", "herdcommand-admin")
                .containsEntry("KEYCLOAK_ADMIN_CLIENT_SECRET", "abc def")
                .containsEntry("DATABASE_USERNAME", "postgres");
    }

    @Test
    void findWalksUpToRepoRoot(@TempDir Path temp) throws Exception {
        Path nested = temp.resolve("apps").resolve("api");
        Files.createDirectories(nested);
        Path env = temp.resolve(".env");
        Files.writeString(env, "API_PORT=8080\n");

        assertThat(LocalDotenv.find(nested)).isEqualTo(env);
    }
}
