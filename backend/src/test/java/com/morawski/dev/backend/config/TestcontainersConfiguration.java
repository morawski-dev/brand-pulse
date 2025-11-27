package com.morawski.dev.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * Provides a shared PostgreSQL container for all tests.
 *
 * This initializer starts PostgreSQL container and configures Spring datasource properties.
 */
public class TestcontainersConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * PostgreSQL container - started once and shared across all tests.
     * Static to ensure single instance per test suite.
     */
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        // Start container when class is loaded
        postgres.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Configure Spring Boot datasource to use Testcontainers PostgreSQL
        TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        ).applyTo(applicationContext.getEnvironment());
    }
}
