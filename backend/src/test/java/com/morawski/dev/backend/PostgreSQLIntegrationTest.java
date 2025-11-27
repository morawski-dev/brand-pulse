package com.morawski.dev.backend;

import com.morawski.dev.backend.config.TestcontainersConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.*;

/**
 * Composite annotation for PostgreSQL integration tests using Testcontainers.
 *
 * This annotation combines:
 * - @DataJpaTest - Spring Data JPA test slice
 * - @AutoConfigureTestDatabase - Disables H2 auto-configuration
 * - @ContextConfiguration - Initializes PostgreSQL container
 *
 * Usage:
 * <pre>
 * {@code
 * @PostgreSQLIntegrationTest
 * class UserRepositoryTest {
 *     @Autowired
 *     private UserRepository userRepository;
 *
 *     @Test
 *     void shouldFindUserByEmail() {
 *         // test implementation
 *     }
 * }
 * }
 * </pre>
 *
 * Note: Extend AbstractIntegrationTest for convenience (optional).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = TestcontainersConfiguration.class)
public @interface PostgreSQLIntegrationTest {
}
