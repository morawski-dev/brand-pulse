package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.AbstractIntegrationTest;
import com.morawski.dev.backend.PostgreSQLIntegrationTest;
import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BrandRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-003: Configuring First Source
 * - US-008: Manual Data Refresh
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("BrandRepository Integration Tests (PostgreSQL)")
class BrandRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // Create test users
        user1 = User.builder()
                .email("user1@example.com")
                .passwordHash("hash123")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(true)
                .build();
        entityManager.persist(user1);

        user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash456")
                .planType(PlanType.PREMIUM)
                .maxSourcesAllowed(10)
                .emailVerified(true)
                .build();
        entityManager.persist(user2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should save and retrieve brand by ID")
    void shouldSaveAndRetrieveBrand() {
        // Given
        Brand brand = createBrand("Test Brand", user1);

        // When
        Brand savedBrand = brandRepository.save(brand);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Brand> foundBrand = brandRepository.findById(savedBrand.getId());
        assertThat(foundBrand).isPresent();
        assertThat(foundBrand.get().getName()).isEqualTo("Test Brand");
        assertThat(foundBrand.get().getUser().getId()).isEqualTo(user1.getId());
        assertThat(foundBrand.get().getCreatedAt()).isNotNull();
        assertThat(foundBrand.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find all brands for a user")
    void shouldFindByUserId() {
        // Given - In MVP, each user can have only one brand (OneToOne relationship)
        Brand brand1 = createBrand("Brand 1", user1);
        Brand brand2 = createBrand("Brand 2", user2);

        entityManager.persist(brand1);
        entityManager.persist(brand2);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Brand> user1Brands = brandRepository.findByUserId(user1.getId());
        List<Brand> user2Brands = brandRepository.findByUserId(user2.getId());

        // Then
        assertThat(user1Brands).hasSize(1);
        assertThat(user1Brands.get(0).getName()).isEqualTo("Brand 1");

        assertThat(user2Brands).hasSize(1);
        assertThat(user2Brands.get(0).getName()).isEqualTo("Brand 2");
    }

    @Test
    @DisplayName("Should find brand by ID and user ID (authorization check)")
    void shouldFindByIdAndUserId() {
        // Given
        Brand brand = createBrand("Test Brand", user1);
        entityManager.persist(brand);
        entityManager.flush();
        Long brandId = brand.getId();
        entityManager.clear();

        // When
        Optional<Brand> foundByCorrectUser = brandRepository.findByIdAndUserId(brandId, user1.getId());
        Optional<Brand> foundByWrongUser = brandRepository.findByIdAndUserId(brandId, user2.getId());

        // Then
        assertThat(foundByCorrectUser).isPresent();
        assertThat(foundByCorrectUser.get().getId()).isEqualTo(brandId);

        assertThat(foundByWrongUser).isEmpty(); // Different user should not see brand
    }

    @Test
    @DisplayName("Should count brands for a user")
    void shouldCountByUserId() {
        // Given - In MVP, each user can have only one brand (OneToOne relationship)
        Brand brand1 = createBrand("Brand 1", user1);
        Brand brand2 = createBrand("Brand 2", user2);

        entityManager.persist(brand1);
        entityManager.persist(brand2);
        entityManager.flush();

        // When
        long user1Count = brandRepository.countByUserId(user1.getId());
        long user2Count = brandRepository.countByUserId(user2.getId());

        // Then
        assertThat(user1Count).isEqualTo(1);
        assertThat(user2Count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check if user has a brand")
    void shouldCheckExistsByUserId() {
        // Given
        Brand brand = createBrand("Test Brand", user1);
        entityManager.persist(brand);
        entityManager.flush();

        // When & Then
        assertThat(brandRepository.existsByUserId(user1.getId())).isTrue();
        assertThat(brandRepository.existsByUserId(user2.getId())).isFalse();
    }

    @Test
    @DisplayName("Should find brand that can perform manual refresh (cooldown passed)")
    void shouldFindByIdAndCanManualRefresh() {
        // Given - Brand with last refresh 25 hours ago (cooldown passed)
        Instant twentyFiveHoursAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        Brand brandCanRefresh = createBrand("Can Refresh", user1);
        brandCanRefresh.setLastManualRefreshAt(twentyFiveHoursAgo);
        entityManager.persist(brandCanRefresh);

        // Given - Brand with last refresh 1 hour ago (cooldown NOT passed)
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Brand brandCannotRefresh = createBrand("Cannot Refresh", user2);
        brandCannotRefresh.setLastManualRefreshAt(oneHourAgo);
        entityManager.persist(brandCannotRefresh);

        // Given - Brand with no refresh history (can refresh) - needs separate user
        User user3 = User.builder()
                .email("user3@example.com")
                .passwordHash("hash789")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(true)
                .build();
        entityManager.persist(user3);

        Brand brandNeverRefreshed = Brand.builder()
                .name("Never Refreshed")
                .user(user3)
                .lastManualRefreshAt(null)
                .build();
        entityManager.persist(brandNeverRefreshed);

        entityManager.flush();
        entityManager.clear();

        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        // When
        Optional<Brand> canRefresh = brandRepository.findByIdAndCanManualRefresh(
                brandCanRefresh.getId(), twentyFourHoursAgo);
        Optional<Brand> cannotRefresh = brandRepository.findByIdAndCanManualRefresh(
                brandCannotRefresh.getId(), twentyFourHoursAgo);
        Optional<Brand> neverRefreshed = brandRepository.findByIdAndCanManualRefresh(
                brandNeverRefreshed.getId(), twentyFourHoursAgo);

        // Then
        assertThat(canRefresh).isPresent(); // 25 hours ago > 24 hours
        assertThat(cannotRefresh).isEmpty(); // 1 hour ago < 24 hours
        assertThat(neverRefreshed).isPresent(); // null means can refresh
    }

    @Test
    @DisplayName("Should find brand with review sources eagerly loaded")
    void shouldFindByIdWithSources() {
        // Given
        Brand brand = createBrand("Test Brand", user1);
        entityManager.persist(brand);

        ReviewSource source1 = ReviewSource.builder()
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://google.com/place/123")
                .externalProfileId("google-123")
                .authMethod(AuthMethod.API)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source1);

        ReviewSource source2 = ReviewSource.builder()
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page/456")
                .externalProfileId("fb-456")
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source2);

        entityManager.flush();
        Long brandId = brand.getId();
        entityManager.clear();

        // When
        Optional<Brand> foundBrand = brandRepository.findByIdWithSources(brandId);

        // Then
        assertThat(foundBrand).isPresent();
        assertThat(foundBrand.get().getReviewSources()).hasSize(2);
        assertThat(foundBrand.get().getReviewSources())
                .extracting(ReviewSource::getSourceType)
                .containsExactlyInAnyOrder(SourceType.GOOGLE, SourceType.FACEBOOK);
    }

    @Test
    @DisplayName("Should find brand by ID and user ID with sources eagerly loaded")
    void shouldFindByIdAndUserIdWithSources() {
        // Given
        Brand brand = createBrand("Test Brand", user1);
        entityManager.persist(brand);

        ReviewSource source = ReviewSource.builder()
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://google.com/place/123")
                .externalProfileId("google-123")
                .authMethod(AuthMethod.API)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source);

        entityManager.flush();
        Long brandId = brand.getId();
        entityManager.clear();

        // When
        Optional<Brand> foundByCorrectUser = brandRepository.findByIdAndUserIdWithSources(
                brandId, user1.getId());
        Optional<Brand> foundByWrongUser = brandRepository.findByIdAndUserIdWithSources(
                brandId, user2.getId());

        // Then
        assertThat(foundByCorrectUser).isPresent();
        assertThat(foundByCorrectUser.get().getReviewSources()).hasSize(1);

        assertThat(foundByWrongUser).isEmpty(); // Different user should not see brand
    }

    @Test
    @DisplayName("Should count all active brands")
    void shouldCountActiveBrands() {
        // Given - Each brand needs separate user (OneToOne relationship)
        Brand brand1 = createBrand("Brand 1", user1);

        User user3 = User.builder()
                .email("user3@example.com")
                .passwordHash("hash789")
                .planType(PlanType.PREMIUM)
                .maxSourcesAllowed(10)
                .emailVerified(true)
                .build();
        entityManager.persist(user3);

        Brand brand2 = createBrand("Brand 2", user2);
        Brand deletedBrand = createBrand("Deleted Brand", user3);
        deletedBrand.softDelete();

        entityManager.persist(brand1);
        entityManager.persist(brand2);
        entityManager.persist(deletedBrand);
        entityManager.flush();

        // When
        long activeCount = brandRepository.countActiveBrands();

        // Then
        assertThat(activeCount).isEqualTo(2); // Only non-deleted brands
    }

    @Test
    @DisplayName("Should not find soft-deleted brands in queries")
    void shouldNotFindSoftDeletedBrands() {
        // Given
        Brand brand = createBrand("Test Brand", user1);
        entityManager.persist(brand);
        entityManager.flush();
        Long brandId = brand.getId();

        brand.softDelete();
        entityManager.flush();
        entityManager.clear();

        // When & Then
        Optional<Brand> foundById = brandRepository.findById(brandId);
        List<Brand> foundByUserId = brandRepository.findByUserId(user1.getId());
        long countByUserId = brandRepository.countByUserId(user1.getId());
        long activeCount = brandRepository.countActiveBrands();

        // @SQLRestriction applies globally - soft-deleted brands are not found
        assertThat(foundById).isEmpty();
        assertThat(foundByUserId).isEmpty();
        assertThat(countByUserId).isEqualTo(0);
        assertThat(activeCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should validate manual refresh cooldown logic")
    void shouldValidateManualRefreshCooldown() {
        // Given
        Instant now = Instant.now();

        Brand brandCanRefresh = createBrand("Can Refresh", user1);
        brandCanRefresh.setLastManualRefreshAt(now.minus(25, ChronoUnit.HOURS));

        Brand brandCannotRefresh = createBrand("Cannot Refresh", user2);
        brandCannotRefresh.setLastManualRefreshAt(now.minus(23, ChronoUnit.HOURS));

        // When & Then
        assertThat(brandCanRefresh.canManualRefresh()).isTrue();
        assertThat(brandCannotRefresh.canManualRefresh()).isFalse();

        Duration remaining = brandCannotRefresh.getTimeUntilNextManualRefresh();
        assertThat(remaining).isGreaterThan(Duration.ZERO);
        assertThat(remaining).isLessThanOrEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("Should not find deleted brands even with soft delete filtering")
    void shouldRespectSQLRestrictionOnAllQueries() {
        // Given - Each brand needs separate user (OneToOne relationship)
        Brand activeBrand = createBrand("Active", user1);

        User user3 = User.builder()
                .email("user3@example.com")
                .passwordHash("hash789")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(true)
                .build();
        entityManager.persist(user3);

        Brand deletedBrand = createBrand("Deleted", user3);
        deletedBrand.softDelete();

        entityManager.persist(activeBrand);
        entityManager.persist(deletedBrand);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Brand> allBrands = brandRepository.findByUserId(user1.getId());
        Optional<Brand> foundWithSources = brandRepository.findByIdWithSources(deletedBrand.getId());

        // Then
        assertThat(allBrands).hasSize(1);
        assertThat(allBrands.get(0).getName()).isEqualTo("Active");
        assertThat(foundWithSources).isEmpty();
    }

    // ===== Helper Methods =====

    private Brand createBrand(String name, User user) {
        return Brand.builder()
                .name(name)
                .user(user)
                .build();
    }
}
