package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.AbstractIntegrationTest;
import com.morawski.dev.backend.PostgreSQLIntegrationTest;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-001: New User Registration
 * - US-002: System Login
 * - User authentication and profile management
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("UserRepository Integration Tests (PostgreSQL)")
class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and retrieve user by ID")
    void shouldSaveAndRetrieveUser() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(false)
                .build();

        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to ensure we're reading from DB

        // Then
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getPlanType()).isEqualTo(PlanType.FREE);
        assertThat(foundUser.get().getMaxSourcesAllowed()).isEqualTo(1);
        assertThat(foundUser.get().getEmailVerified()).isFalse();
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
        assertThat(foundUser.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email (case-insensitive)")
    void shouldFindUserByEmailIgnoreCase() {
        // Given
        User user = createUser("Test@Example.COM", "hash123");
        user = userRepository.saveAndFlush(user);
        Long userId = user.getId();
        entityManager.clear();

        // When
        Optional<User> foundByLowerCase = userRepository.findByEmailIgnoreCase("test@example.com");
        Optional<User> foundByUpperCase = userRepository.findByEmailIgnoreCase("TEST@EXAMPLE.COM");
        Optional<User> foundByMixedCase = userRepository.findByEmailIgnoreCase("Test@Example.Com");

        // Then
        assertThat(foundByLowerCase).isPresent();
        assertThat(foundByUpperCase).isPresent();
        assertThat(foundByMixedCase).isPresent();
        assertThat(foundByLowerCase.get().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should check if email exists (case-insensitive)")
    void shouldCheckIfEmailExists() {
        // Given
        User user = createUser("existing@example.com", "hash123");
        userRepository.saveAndFlush(user);

        // When & Then
        assertThat(userRepository.existsByEmailIgnoreCase("existing@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("EXISTING@EXAMPLE.COM")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should find user by verification token")
    void shouldFindUserByVerificationToken() {
        // Given
        User user = User.builder()
                .email("verify@example.com")
                .passwordHash("hash123")
                .verificationToken("token-12345")
                .emailVerified(false)
                .build();
        userRepository.saveAndFlush(user);
        entityManager.clear();

        // When
        Optional<User> foundUser = userRepository.findByVerificationToken("token-12345");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("verify@example.com");
        assertThat(foundUser.get().getVerificationToken()).isEqualTo("token-12345");
    }

    @Test
    @DisplayName("Should find user by password reset token")
    void shouldFindUserByPasswordResetToken() {
        // Given
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        User user = User.builder()
                .email("reset@example.com")
                .passwordHash("hash123")
                .passwordResetToken("reset-token-123")
                .passwordResetExpiresAt(expiresAt)
                .build();
        userRepository.saveAndFlush(user);
        entityManager.clear();

        // When
        Optional<User> foundUser = userRepository.findByPasswordResetToken("reset-token-123");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getPasswordResetToken()).isEqualTo("reset-token-123");
    }

    @Test
    @DisplayName("Should find user by valid password reset token (not expired)")
    void shouldFindUserByValidPasswordResetToken() {
        // Given
        Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);

        User validUser = User.builder()
                .email("valid@example.com")
                .passwordHash("hash123")
                .passwordResetToken("valid-token")
                .passwordResetExpiresAt(futureExpiry)
                .build();

        User expiredUser = User.builder()
                .email("expired@example.com")
                .passwordHash("hash123")
                .passwordResetToken("expired-token")
                .passwordResetExpiresAt(pastExpiry)
                .build();

        userRepository.save(validUser);
        userRepository.save(expiredUser);
        userRepository.flush();
        entityManager.clear();

        // When
        Optional<User> foundValidUser = userRepository.findByValidPasswordResetToken("valid-token", Instant.now());
        Optional<User> foundExpiredUser = userRepository.findByValidPasswordResetToken("expired-token", Instant.now());

        // Then
        assertThat(foundValidUser).isPresent();
        assertThat(foundValidUser.get().getEmail()).isEqualTo("valid@example.com");
        assertThat(foundExpiredUser).isEmpty(); // Expired token should not be found
    }

    @Test
    @DisplayName("Should count active users (excluding soft-deleted)")
    void shouldCountActiveUsers() {
        // Given
        User activeUser1 = createUser("active1@example.com", "hash1");
        User activeUser2 = createUser("active2@example.com", "hash2");
        User deletedUser = createUser("deleted@example.com", "hash3");
        deletedUser.softDelete();

        userRepository.save(activeUser1);
        userRepository.save(activeUser2);
        userRepository.save(deletedUser);
        userRepository.flush();

        // When
        long activeCount = userRepository.countActiveUsers();

        // Then
        assertThat(activeCount).isEqualTo(2); // Only active users
    }

    @Test
    @DisplayName("Should find verified users only")
    void shouldFindVerifiedUsers() {
        // Given
        User verifiedUser1 = createUser("verified1@example.com", "hash1");
        verifiedUser1.setEmailVerified(true);

        User verifiedUser2 = createUser("verified2@example.com", "hash2");
        verifiedUser2.setEmailVerified(true);

        User unverifiedUser = createUser("unverified@example.com", "hash3");
        unverifiedUser.setEmailVerified(false);

        userRepository.save(verifiedUser1);
        userRepository.save(verifiedUser2);
        userRepository.save(unverifiedUser);
        userRepository.flush();

        // When
        List<User> verifiedUsers = userRepository.findVerifiedUsers();

        // Then
        assertThat(verifiedUsers).hasSize(2);
        assertThat(verifiedUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("verified1@example.com", "verified2@example.com");
    }

    @Test
    @DisplayName("Should find users by plan type")
    void shouldFindUsersByPlanType() {
        // Given
        User freeUser1 = createUser("free1@example.com", "hash1");
        freeUser1.setPlanType(PlanType.FREE);
        freeUser1.setMaxSourcesAllowed(1);

        User freeUser2 = createUser("free2@example.com", "hash2");
        freeUser2.setPlanType(PlanType.FREE);
        freeUser2.setMaxSourcesAllowed(1);

        User premiumUser = createUser("premium@example.com", "hash3");
        premiumUser.setPlanType(PlanType.PREMIUM);
        premiumUser.setMaxSourcesAllowed(10);

        userRepository.save(freeUser1);
        userRepository.save(freeUser2);
        userRepository.save(premiumUser);
        userRepository.flush();

        // When
        List<User> freeUsers = userRepository.findByPlanType(PlanType.FREE);
        List<User> premiumUsers = userRepository.findByPlanType(PlanType.PREMIUM);

        // Then
        assertThat(freeUsers).hasSize(2);
        assertThat(freeUsers).extracting(User::getPlanType).containsOnly(PlanType.FREE);

        assertThat(premiumUsers).hasSize(1);
        assertThat(premiumUsers.get(0).getPlanType()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    @DisplayName("Should not find soft-deleted users in queries")
    void shouldNotFindSoftDeletedUsers() {
        // Given
        User user = createUser("todelete@example.com", "hash123");
        user = userRepository.saveAndFlush(user);
        Long userId = user.getId();

        user.softDelete();
        userRepository.flush();
        entityManager.clear();

        // When & Then
        Optional<User> foundByEmail = userRepository.findByEmailIgnoreCase("todelete@example.com");
        Optional<User> foundById = userRepository.findById(userId);
        long activeCount = userRepository.countActiveUsers();

        // @SQLRestriction applies globally - soft-deleted users are not found by ANY query
        assertThat(foundByEmail).isEmpty();
        assertThat(foundById).isEmpty(); // @SQLRestriction filters even findById
        assertThat(activeCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should validate password reset token expiration")
    void shouldValidatePasswordResetTokenExpiration() {
        // Given
        Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);

        User userWithValidToken = User.builder()
                .email("valid@example.com")
                .passwordHash("hash123")
                .passwordResetToken("valid-token")
                .passwordResetExpiresAt(futureExpiry)
                .build();

        User userWithExpiredToken = User.builder()
                .email("expired@example.com")
                .passwordHash("hash123")
                .passwordResetToken("expired-token")
                .passwordResetExpiresAt(pastExpiry)
                .build();

        userRepository.save(userWithValidToken);
        userRepository.save(userWithExpiredToken);
        userRepository.flush();

        // When & Then
        assertThat(userWithValidToken.isPasswordResetTokenValid()).isTrue();
        assertThat(userWithExpiredToken.isPasswordResetTokenValid()).isFalse();
    }

    // ===== Helper Methods =====

    private User createUser(String email, String passwordHash) {
        return User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(false)
                .build();
    }
}
