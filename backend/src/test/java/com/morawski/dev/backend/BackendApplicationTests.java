package com.morawski.dev.backend;

import com.morawski.dev.backend.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Smoke test verifying the full Spring application context loads.
 *
 * Boots the complete context (security, scheduling, JPA, OpenRouter, etc.) against
 * a Testcontainers PostgreSQL instance — the same database the app uses in production,
 * so JSONB columns and the real entity model are exercised. Schema is created from the
 * entities via {@code ddl-auto=create-drop} (Liquibase is disabled for tests).
 */
@SpringBootTest
@ContextConfiguration(initializers = TestcontainersConfiguration.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
