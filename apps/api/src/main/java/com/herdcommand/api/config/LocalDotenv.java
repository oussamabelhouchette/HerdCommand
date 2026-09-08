package com.herdcommand.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads repo-root {@code .env} into system properties for {@code spring-boot:run}.
 * Existing environment variables win. Tests never call this.
 */
public final class LocalDotenv {

    private static final Logger log = LoggerFactory.getLogger(LocalDotenv.class);

    private LocalDotenv() {}

    public static void load() {
        Path file = find(Path.of("").toAbsolutePath());
        if (file == null) {
            return;
        }
        Map<String, String> values;
        try {
            values = parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.warn("Could not read local .env from {}", file.toAbsolutePath());
            return;
        }
        int applied = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (present(System.getenv(entry.getKey())) || present(System.getProperty(entry.getKey()))) {
                continue;
            }
            System.setProperty(entry.getKey(), entry.getValue());
            applied++;
        }
        log.info("Loaded {} local setting(s) from {}", applied, file.toAbsolutePath());
    }

    static Path find(Path start) {
        Path dir = start;
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    static Map<String, String> parse(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("export ")) {
                trimmed = trimmed.substring("export ".length()).trim();
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                continue;
            }
            values.put(key, unquote(trimmed.substring(eq + 1).trim()));
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
