package com.herdcommand.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
                .components(new Components().addSecuritySchemes(
                        "bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
