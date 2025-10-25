package com.morawski.dev.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for BrandPulse REST API documentation.
 *
 * API Plan Section 18: API Documentation
 *
 * Features:
 * - Interactive API explorer (Swagger UI)
 * - Request/response examples
 * - JWT Bearer authentication setup
 * - Try-it-out functionality
 *
 * Endpoints:
 * - Swagger UI: http://localhost:8080/swagger-ui/index.html
 * - OpenAPI Spec: http://localhost:8080/api-docs
 */
@Slf4j
@Configuration
public class OpenAPIConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Configure OpenAPI documentation bean.
     *
     * Includes:
     * - API metadata (title, version, description)
     * - JWT authentication scheme
     * - Server URLs for different environments
     * - Contact and license information
     *
     * @return Configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        log.info("Configuring OpenAPI documentation");

        return new OpenAPI()
            .info(apiInfo())
            .servers(apiServers())
            .components(securityComponents())
            .addSecurityItem(securityRequirement());
    }

    /**
     * API metadata information.
     *
     * @return API Info
     */
    private Info apiInfo() {
        return new Info()
            .title("BrandPulse REST API")
            .version("v1")
            .description("""
                BrandPulse REST API provides backend services for monitoring and analyzing customer reviews
                from multiple sources (Google, Facebook, Trustpilot).

                ## Features
                - User authentication with JWT
                - Review aggregation from multiple sources
                - AI-powered sentiment analysis
                - Dashboard with metrics and insights
                - Manual and automated review synchronization

                ## Authentication
                Most endpoints require JWT Bearer token authentication.

                To authenticate:
                1. Register: POST /api/auth/register
                2. Login: POST /api/auth/login (returns JWT token)
                3. Use token in Authorization header: Bearer {token}

                ## API Version
                Current API version: v1
                Base URL: /api

                ## Rate Limits
                - Per User: 1000 requests/hour
                - Login: 5 requests/15 minutes
                - Manual Sync: 1 request/24 hours
                """)
            .contact(apiContact())
            .license(apiLicense());
    }

    /**
     * API contact information.
     *
     * @return Contact info
     */
    private Contact apiContact() {
        return new Contact()
            .name("BrandPulse Support")
            .email("support@brandpulse.io")
            .url("https://brandpulse.io");
    }

    /**
     * API license information.
     *
     * @return License info
     */
    private License apiLicense() {
        return new License()
            .name("Proprietary")
            .url("https://brandpulse.io/terms");
    }

    /**
     * API server configurations for different environments.
     *
     * @return List of server configurations
     */
    private List<Server> apiServers() {
        return List.of(
            new Server()
                .url("http://localhost:8080")
                .description("Development server"),
            new Server()
                .url("https://api-staging.brandpulse.io")
                .description("Staging server"),
            new Server()
                .url("https://api.brandpulse.io")
                .description("Production server")
        );
    }

    /**
     * Security components - JWT authentication scheme.
     *
     * API Plan Section 12.1: JWT Bearer token authentication
     *
     * @return Security components
     */
    private Components securityComponents() {
        return new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                    JWT Bearer token authentication.

                    Token format: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

                    Token expiration: 60 minutes (configurable)

                    To obtain token:
                    1. Register: POST /api/auth/register
                    2. Login: POST /api/auth/login

                    Token structure:
                    {
                      "sub": "1",
                      "email": "user@example.com",
                      "planType": "FREE",
                      "maxSourcesAllowed": 1,
                      "iat": 1642598400,
                      "exp": 1642602000
                    }
                    """)
            );
    }

    /**
     * Global security requirement - apply JWT auth to all endpoints.
     *
     * Note: Public endpoints (login, register, etc.) are configured
     * in SecurityConfig to override this requirement.
     *
     * @return Security requirement
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }
}