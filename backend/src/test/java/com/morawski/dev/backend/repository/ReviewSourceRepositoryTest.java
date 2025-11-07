package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.AbstractIntegrationTest;
import com.morawski.dev.backend.PostgreSQLIntegrationTest;
import com.morawski.dev.backend.dto.common.*;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
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
 * Integration tests for {@link ReviewSourceRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-003: Configuring First Source
 * - US-006: Switching Between Locations
 * - US-009: Free Plan Limitation
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("ReviewSourceRepository Integration Tests (PostgreSQL)")
class ReviewSourceRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ReviewSourceRepository reviewSourceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Brand brand;

    @BeforeEach
    void setUp() {
        // Create test data hierarchy: User -> Brand
        user = User.builder()
                .email("test@example.com")
                .passwordHash("hash123")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(true)
                .build();
        entityManager.persist(user);

        brand = Brand.builder()
                .name("Test Brand")
                .user(user)
                .build();
        entityManager.persist(brand);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should save and retrieve review source by ID")
    void shouldSaveAndRetrieveReviewSource() {
        // Given
        ReviewSource source = createGoogleSource("google-123");

        // When
        ReviewSource savedSource = reviewSourceRepository.save(source);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<ReviewSource> foundSource = reviewSourceRepository.findById(savedSource.getId());
        assertThat(foundSource).isPresent();
        assertThat(foundSource.get().getSourceType()).isEqualTo(SourceType.GOOGLE);
        assertThat(foundSource.get().getExternalProfileId()).isEqualTo("google-123");
        assertThat(foundSource.get().getAuthMethod()).isEqualTo(AuthMethod.API);
        assertThat(foundSource.get().getIsActive()).isTrue();
        assertThat(foundSource.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find all review sources for a brand")
    void shouldFindByBrandId() {
        // Given
        ReviewSource googleSource = createGoogleSource("google-123");
        ReviewSource facebookSource = createFacebookSource("fb-456");
        ReviewSource trustpilotSource = createTrustpilotSource("tp-789");

        entityManager.persist(googleSource);
        entityManager.persist(facebookSource);
        entityManager.persist(trustpilotSource);
        entityManager.flush();
        entityManager.clear();

        // When
        List<ReviewSource> sources = reviewSourceRepository.findByBrandId(brand.getId());

        // Then
        assertThat(sources).hasSize(3);
        assertThat(sources).extracting(ReviewSource::getSourceType)
                .containsExactlyInAnyOrder(SourceType.GOOGLE, SourceType.FACEBOOK, SourceType.TRUSTPILOT);
    }

    @Test
    @DisplayName("Should find review source by ID and brand ID (authorization check)")
    void shouldFindByIdAndBrandId() {
        // Given - Create second user and brand (OneToOne relationship)
        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash456")
                .planType(PlanType.PREMIUM)
                .maxSourcesAllowed(10)
                .emailVerified(true)
                .build();
        entityManager.persist(user2);

        Brand brand2 = Brand.builder()
                .name("Other Brand")
                .user(user2)
                .build();
        entityManager.persist(brand2);

        ReviewSource source = createGoogleSource("google-123");
        entityManager.persist(source);
        entityManager.flush();
        Long sourceId = source.getId();
        entityManager.clear();

        // When
        Optional<ReviewSource> foundByCorrectBrand = reviewSourceRepository.findByIdAndBrandId(
                sourceId, brand.getId());
        Optional<ReviewSource> foundByWrongBrand = reviewSourceRepository.findByIdAndBrandId(
                sourceId, brand2.getId());

        // Then
        assertThat(foundByCorrectBrand).isPresent();
        assertThat(foundByCorrectBrand.get().getId()).isEqualTo(sourceId);

        assertThat(foundByWrongBrand).isEmpty(); // Different brand should not see source
    }

    @Test
    @DisplayName("Should count active review sources for a brand")
    void shouldCountByBrandIdAndDeletedAtIsNull() {
        // Given
        ReviewSource activeSource1 = createGoogleSource("google-123");
        ReviewSource activeSource2 = createFacebookSource("fb-456");
        ReviewSource deletedSource = createTrustpilotSource("tp-789");
        deletedSource.softDelete();

        entityManager.persist(activeSource1);
        entityManager.persist(activeSource2);
        entityManager.persist(deletedSource);
        entityManager.flush();

        // When
        long count = reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brand.getId());

        // Then
        assertThat(count).isEqualTo(2); // Only active sources
    }

    @Test
    @DisplayName("Should check if duplicate source exists")
    void shouldCheckExistsByBrandIdAndSourceTypeAndExternalProfileId() {
        // Given
        ReviewSource source = createGoogleSource("google-123");
        entityManager.persist(source);
        entityManager.flush();

        // When & Then
        assertThat(reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(
                brand.getId(), SourceType.GOOGLE, "google-123")).isTrue();

        assertThat(reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(
                brand.getId(), SourceType.GOOGLE, "google-999")).isFalse();

        assertThat(reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(
                brand.getId(), SourceType.FACEBOOK, "google-123")).isFalse();
    }

    @Test
    @DisplayName("Should find review source by brand, type, and external ID")
    void shouldFindByBrandIdAndSourceTypeAndExternalProfileId() {
        // Given
        ReviewSource source = createGoogleSource("google-123");
        entityManager.persist(source);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<ReviewSource> found = reviewSourceRepository.findByBrandIdAndSourceTypeAndExternalProfileId(
                brand.getId(), SourceType.GOOGLE, "google-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getExternalProfileId()).isEqualTo("google-123");
    }

    @Test
    @DisplayName("Should find all active review sources")
    void shouldFindByIsActiveTrue() {
        // Given
        ReviewSource activeSource = createGoogleSource("google-123");
        activeSource.setIsActive(true);

        ReviewSource inactiveSource = createFacebookSource("fb-456");
        inactiveSource.setIsActive(false);

        entityManager.persist(activeSource);
        entityManager.persist(inactiveSource);
        entityManager.flush();

        // When
        List<ReviewSource> activeSources = reviewSourceRepository.findByIsActiveTrue();

        // Then
        assertThat(activeSources).hasSize(1);
        assertThat(activeSources.get(0).getExternalProfileId()).isEqualTo("google-123");
    }

    @Test
    @DisplayName("Should find sources ready for scheduled sync")
    void shouldFindByNextScheduledSyncAtBeforeAndIsActiveTrue() {
        // Given
        Instant now = Instant.now();
        Instant past = now.minus(1, ChronoUnit.HOURS);
        Instant future = now.plus(1, ChronoUnit.HOURS);

        ReviewSource sourceReadyForSync = createGoogleSource("google-123");
        sourceReadyForSync.setNextScheduledSyncAt(past);
        sourceReadyForSync.setIsActive(true);

        ReviewSource sourceNotReadyYet = createFacebookSource("fb-456");
        sourceNotReadyYet.setNextScheduledSyncAt(future);
        sourceNotReadyYet.setIsActive(true);

        ReviewSource inactiveSource = createTrustpilotSource("tp-789");
        inactiveSource.setNextScheduledSyncAt(past);
        inactiveSource.setIsActive(false);

        entityManager.persist(sourceReadyForSync);
        entityManager.persist(sourceNotReadyYet);
        entityManager.persist(inactiveSource);
        entityManager.flush();

        // When
        List<ReviewSource> readySources = reviewSourceRepository.findByNextScheduledSyncAtBeforeAndIsActiveTrue(now);

        // Then
        assertThat(readySources).hasSize(1);
        assertThat(readySources.get(0).getExternalProfileId()).isEqualTo("google-123");
    }

    @Test
    @DisplayName("Should find sources by sync status")
    void shouldFindByLastSyncStatus() {
        // Given
        ReviewSource successSource = createGoogleSource("google-123");
        successSource.setLastSyncStatus(SyncStatus.SUCCESS);

        ReviewSource failedSource = createFacebookSource("fb-456");
        failedSource.setLastSyncStatus(SyncStatus.FAILED);

        ReviewSource inProgressSource = createTrustpilotSource("tp-789");
        inProgressSource.setLastSyncStatus(SyncStatus.IN_PROGRESS);

        entityManager.persist(successSource);
        entityManager.persist(failedSource);
        entityManager.persist(inProgressSource);
        entityManager.flush();

        // When
        List<ReviewSource> successSources = reviewSourceRepository.findByLastSyncStatus(SyncStatus.SUCCESS);
        List<ReviewSource> failedSources = reviewSourceRepository.findByLastSyncStatus(SyncStatus.FAILED);
        List<ReviewSource> inProgressSources = reviewSourceRepository.findByLastSyncStatus(SyncStatus.IN_PROGRESS);

        // Then
        assertThat(successSources).hasSize(1);
        assertThat(successSources.get(0).getSourceType()).isEqualTo(SourceType.GOOGLE);

        assertThat(failedSources).hasSize(1);
        assertThat(failedSources.get(0).getSourceType()).isEqualTo(SourceType.FACEBOOK);

        assertThat(inProgressSources).hasSize(1);
        assertThat(inProgressSources.get(0).getSourceType()).isEqualTo(SourceType.TRUSTPILOT);
    }

    @Test
    @DisplayName("Should find sources with failed sync status")
    void shouldFindFailedSources() {
        // Given
        ReviewSource failedActiveSource = createGoogleSource("google-123");
        failedActiveSource.setLastSyncStatus(SyncStatus.FAILED);
        failedActiveSource.setIsActive(true);

        ReviewSource failedInactiveSource = createFacebookSource("fb-456");
        failedInactiveSource.setLastSyncStatus(SyncStatus.FAILED);
        failedInactiveSource.setIsActive(false);

        ReviewSource successSource = createTrustpilotSource("tp-789");
        successSource.setLastSyncStatus(SyncStatus.SUCCESS);
        successSource.setIsActive(true);

        entityManager.persist(failedActiveSource);
        entityManager.persist(failedInactiveSource);
        entityManager.persist(successSource);
        entityManager.flush();

        // When
        List<ReviewSource> failedSources = reviewSourceRepository.findFailedSources();

        // Then
        assertThat(failedSources).hasSize(1);
        assertThat(failedSources.get(0).getExternalProfileId()).isEqualTo("google-123");
    }

    @Test
    @DisplayName("Should find all sources for a user (across brands)")
    void shouldFindByUserId() {
        // Given - In MVP, user has only one brand (OneToOne), so this test verifies sources from that brand
        ReviewSource sourceFromBrand1 = createGoogleSource("google-123");
        sourceFromBrand1.setBrand(brand);
        entityManager.persist(sourceFromBrand1);

        ReviewSource sourceFromBrand2 = createFacebookSource("fb-456");
        sourceFromBrand2.setBrand(brand);
        entityManager.persist(sourceFromBrand2);

        entityManager.flush();
        entityManager.clear();

        // When
        List<ReviewSource> userSources = reviewSourceRepository.findByUserId(user.getId());

        // Then
        assertThat(userSources).hasSize(2);
        assertThat(userSources).extracting(ReviewSource::getSourceType)
                .containsExactlyInAnyOrder(SourceType.GOOGLE, SourceType.FACEBOOK);
    }

    @Test
    @DisplayName("Should count sources by type")
    void shouldCountBySourceType() {
        // Given - Create sources across different brands
        ReviewSource googleSource1 = createGoogleSource("google-123");

        // Create second user and brand for second Google source
        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash456")
                .planType(PlanType.PREMIUM)
                .maxSourcesAllowed(10)
                .emailVerified(true)
                .build();
        entityManager.persist(user2);

        Brand brand2 = Brand.builder()
                .name("Second Brand")
                .user(user2)
                .build();
        entityManager.persist(brand2);

        ReviewSource googleSource2 = createGoogleSource("google-456");
        googleSource2.setBrand(brand2);

        ReviewSource facebookSource = createFacebookSource("fb-789");

        entityManager.persist(googleSource1);
        entityManager.persist(googleSource2);
        entityManager.persist(facebookSource);
        entityManager.flush();

        // When
        long googleCount = reviewSourceRepository.countBySourceType(SourceType.GOOGLE);
        long facebookCount = reviewSourceRepository.countBySourceType(SourceType.FACEBOOK);
        long trustpilotCount = reviewSourceRepository.countBySourceType(SourceType.TRUSTPILOT);

        // Then
        assertThat(googleCount).isEqualTo(2);
        assertThat(facebookCount).isEqualTo(1);
        assertThat(trustpilotCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not find soft-deleted review sources")
    void shouldNotFindSoftDeletedSources() {
        // Given
        ReviewSource activeSource = createGoogleSource("google-123");
        ReviewSource deletedSource = createFacebookSource("fb-456");
        deletedSource.softDelete();

        entityManager.persist(activeSource);
        entityManager.persist(deletedSource);
        entityManager.flush();
        Long deletedId = deletedSource.getId();
        entityManager.clear();

        // When
        Optional<ReviewSource> foundById = reviewSourceRepository.findById(deletedId);
        List<ReviewSource> foundByBrand = reviewSourceRepository.findByBrandId(brand.getId());
        long count = reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(brand.getId());

        // Then
        assertThat(foundById).isEmpty(); // @SQLRestriction filters soft-deleted
        assertThat(foundByBrand).hasSize(1);
        assertThat(foundByBrand.get(0).getSourceType()).isEqualTo(SourceType.GOOGLE);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should validate sync status tracking")
    void shouldTrackSyncStatus() {
        // Given
        ReviewSource source = createGoogleSource("google-123");
        source.setLastSyncStatus(SyncStatus.SUCCESS);
        source.setLastSyncAt(Instant.now());
        source.setLastSyncError(null);
        entityManager.persist(source);
        entityManager.flush();

        // When & Then
        assertThat(source.isLastSyncSuccessful()).isTrue();

        source.setLastSyncStatus(SyncStatus.FAILED);
        source.setLastSyncError("API rate limit exceeded");

        assertThat(source.isLastSyncSuccessful()).isFalse();
        assertThat(source.getLastSyncError()).isEqualTo("API rate limit exceeded");
    }

    @Test
    @DisplayName("Should enforce unique constraint on brand + source type + external ID")
    void shouldEnforceUniqueConstraint() {
        // Given
        ReviewSource source1 = createGoogleSource("google-123");
        entityManager.persist(source1);
        entityManager.flush();

        // When & Then - duplicate should be detected
        boolean exists = reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(
                brand.getId(), SourceType.GOOGLE, "google-123");

        assertThat(exists).isTrue();

        // Different external ID should not be duplicate
        boolean notExists = reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(
                brand.getId(), SourceType.GOOGLE, "google-999");

        assertThat(notExists).isFalse();
    }

    // ===== Helper Methods =====

    private ReviewSource createGoogleSource(String externalId) {
        return ReviewSource.builder()
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://maps.google.com/place/" + externalId)
                .externalProfileId(externalId)
                .authMethod(AuthMethod.API)
                .isActive(true)
                .brand(brand)
                .build();
    }

    private ReviewSource createFacebookSource(String externalId) {
        return ReviewSource.builder()
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page/" + externalId)
                .externalProfileId(externalId)
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
    }

    private ReviewSource createTrustpilotSource(String externalId) {
        return ReviewSource.builder()
                .sourceType(SourceType.TRUSTPILOT)
                .profileUrl("https://trustpilot.com/review/" + externalId)
                .externalProfileId(externalId)
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
    }
}
