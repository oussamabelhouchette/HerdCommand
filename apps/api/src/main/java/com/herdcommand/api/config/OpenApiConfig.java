package com.herdcommand.api.config;

import com.herdcommand.api.api.error.ApiError;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI herdCommandOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HerdCommand API")
                        .version("v1")
                        .description("REST contract for HerdCommand. Identifiers are UUIDs. Timestamps are ISO-8601 UTC. Pagination uses page, size and sort."))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSchemas("ApiError", new Schema<ApiError>()
                                .description("Standard error envelope for validation, not-found, conflict, unauthorized and forbidden responses.")
                                .addProperty("code", new Schema<String>().example("BREED_CODE_ALREADY_EXISTS"))
                                .addProperty("message", new Schema<String>().example("A breed with this code already exists."))
                                .addProperty("fieldErrors", new Schema<>().example(List.of(
                                        Map.of("field", "code", "message", "Code must be unique."))))
                                .addProperty("timestamp", new Schema<String>().example("2026-09-04T12:00:00Z"))
                                .addProperty("path", new Schema<String>().example("/api/v1/admin/animal-breeds"))));
    }
}
