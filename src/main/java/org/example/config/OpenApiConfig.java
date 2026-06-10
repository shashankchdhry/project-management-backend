package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pmPlatformOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PM Platform API")
                .version("v1")
                .description("Jira-like project management platform backend. See docs/ for design."));
    }
}
