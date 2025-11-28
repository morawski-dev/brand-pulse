package com.morawski.dev.backend;

/**
 * Abstract base class for integration tests using Testcontainers with PostgreSQL.
 *
 * Simply extend this class for convenience. Use @PostgreSQLIntegrationTest annotation
 * on your test class to enable PostgreSQL Testcontainers.
 *
 * Usage:
 * <pre>
 * {@code
 * @PostgreSQLIntegrationTest
 * class UserRepositoryTest extends AbstractIntegrationTest {
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
 * Note: The @PostgreSQLIntegrationTest annotation configures PostgreSQL container
 * via TestcontainersConfiguration initializer.
 */
public abstract class AbstractIntegrationTest {
    // PostgreSQL container is configured by @PostgreSQLIntegrationTest annotation
}
