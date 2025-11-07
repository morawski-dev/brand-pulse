package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.AbstractIntegrationTest;
import com.morawski.dev.backend.PostgreSQLIntegrationTest;
import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.DashboardAggregate;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DashboardAggregateRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-004: Viewing Aggregated Reviews
 * - US-006: Switching Between Locations
 * - Dashboard performance optimization
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("DashboardAggregateRepository Integration Tests (PostgreSQL)")
class DashboardAggregateRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private DashboardAggregateRepository dashboardAggregateRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Brand brand;
    private ReviewSource reviewSource;

    @BeforeEach
    void setUp() {
        // Create test data hierarchy: User -> Brand -> ReviewSource
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

        reviewSource = ReviewSource.builder()
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://maps.google.com/place/123")
                .externalProfileId("google-123")
                .authMethod(AuthMethod.API)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(reviewSource);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should save and retrieve dashboard aggregate by ID")
    void shouldSaveAndRetrieveAggregate() {
        // Given
        DashboardAggregate aggregate = createAggregate(LocalDate.now(), 100, BigDecimal.valueOf(4.5),
                70, 20, 10);

        // When
        DashboardAggregate saved = dashboardAggregateRepository.save(aggregate);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<DashboardAggregate> found = dashboardAggregateRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTotalReviews()).isEqualTo(100);
        assertThat(found.get().getAvgRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(found.get().getPositiveCount()).isEqualTo(70);
        assertThat(found.get().getNegativeCount()).isEqualTo(20);
        assertThat(found.get().getNeutralCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should find aggregate by review source ID and date")
    void shouldFindByReviewSourceIdAndDate() {
        // Given
        LocalDate today = LocalDate.now();
        DashboardAggregate aggregate = createAggregate(today, 50, BigDecimal.valueOf(4.0),
                30, 10, 10);
        entityManager.persist(aggregate);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<DashboardAggregate> found = dashboardAggregateRepository
                .findByReviewSourceIdAndDate(reviewSource.getId(), today);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDate()).isEqualTo(today);
        assertThat(found.get().getTotalReviews()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should find aggregates for review source within date range")
    void shouldFindByReviewSourceIdAndDateBetween() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate threeDaysAgo = today.minusDays(3);
        LocalDate sevenDaysAgo = today.minusDays(7);

        DashboardAggregate agg1 = createAggregate(today, 50, BigDecimal.valueOf(4.5), 40, 5, 5);
        DashboardAggregate agg2 = createAggregate(threeDaysAgo, 30, BigDecimal.valueOf(4.0), 20, 5, 5);
        DashboardAggregate agg3 = createAggregate(sevenDaysAgo, 20, BigDecimal.valueOf(3.5), 10, 5, 5);

        entityManager.persist(agg1);
        entityManager.persist(agg2);
        entityManager.persist(agg3);
        entityManager.flush();
        entityManager.clear();

        // When - query for last 5 days
        LocalDate fiveDaysAgo = today.minusDays(5);
        List<DashboardAggregate> aggregates = dashboardAggregateRepository
                .findByReviewSourceIdAndDateBetween(reviewSource.getId(), fiveDaysAgo, today);

        // Then
        assertThat(aggregates).hasSize(2); // Only today and 3 days ago
        assertThat(aggregates).extracting(DashboardAggregate::getTotalReviews)
                .containsExactly(30, 50); // Ordered by date ASC (3 days ago, then today)
    }

    @Test
    @DisplayName("Should find aggregates for brand (across all sources) within date range")
    void shouldFindByBrandIdAndDateBetween() {
        // Given - Create second review source
        ReviewSource source2 = ReviewSource.builder()
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page/456")
                .externalProfileId("fb-456")
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source2);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        DashboardAggregate agg1 = createAggregate(reviewSource, today, 50, BigDecimal.valueOf(4.5), 40, 5, 5);
        DashboardAggregate agg2 = createAggregate(source2, today, 30, BigDecimal.valueOf(4.0), 20, 5, 5);
        DashboardAggregate agg3 = createAggregate(reviewSource, yesterday, 20, BigDecimal.valueOf(3.8), 15, 3, 2);

        entityManager.persist(agg1);
        entityManager.persist(agg2);
        entityManager.persist(agg3);
        entityManager.flush();
        entityManager.clear();

        // When
        List<DashboardAggregate> brandAggregates = dashboardAggregateRepository
                .findByBrandIdAndDateBetween(brand.getId(), yesterday, today);

        // Then
        assertThat(brandAggregates).hasSize(3);
    }

    @Test
    @DisplayName("Should find recent aggregates for review source")
    void shouldFindRecentByReviewSourceId() {
        // Given
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 10; i++) {
            LocalDate date = today.minusDays(i);
            DashboardAggregate agg = createAggregate(date, 50, BigDecimal.valueOf(4.0), 30, 10, 10);
            entityManager.persist(agg);
        }
        entityManager.flush();
        entityManager.clear();

        // When - get last 7 days
        LocalDate sevenDaysAgo = today.minusDays(7);
        List<DashboardAggregate> recent = dashboardAggregateRepository
                .findRecentByReviewSourceId(reviewSource.getId(), sevenDaysAgo);

        // Then
        assertThat(recent).hasSize(8); // Today + 7 days ago = 8 days
    }

    @Test
    @DisplayName("Should find aggregates needing recalculation (stale data)")
    void shouldFindAggregatesNeedingRecalculation() {
        // Given
        Instant now = Instant.now();
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        Instant thirtyMinutesAgo = now.minus(30, ChronoUnit.MINUTES);

        DashboardAggregate staleAgg = createAggregate(LocalDate.now(), 50, BigDecimal.valueOf(4.0), 30, 10, 10);
        staleAgg.setLastCalculatedAt(twoHoursAgo);

        DashboardAggregate freshAgg = createAggregate(LocalDate.now().minusDays(1), 30, BigDecimal.valueOf(4.5), 20, 5, 5);
        freshAgg.setLastCalculatedAt(thirtyMinutesAgo);

        entityManager.persist(staleAgg);
        entityManager.persist(freshAgg);
        entityManager.flush();
        entityManager.clear();

        // When - find aggregates older than 1 hour
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        List<DashboardAggregate> needsRecalc = dashboardAggregateRepository
                .findAggregatesNeedingRecalculation(oneHourAgo);

        // Then
        assertThat(needsRecalc).hasSize(1);
        assertThat(needsRecalc.get(0).getLastCalculatedAt()).isBefore(oneHourAgo);
    }

    @Test
    @DisplayName("Should calculate totals for review source")
    void shouldCalculateTotalsForSource() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        DashboardAggregate agg1 = createAggregate(today, 50, BigDecimal.valueOf(4.5), 40, 5, 5);
        DashboardAggregate agg2 = createAggregate(yesterday, 30, BigDecimal.valueOf(4.0), 20, 5, 5);
        DashboardAggregate agg3 = createAggregate(twoDaysAgo, 20, BigDecimal.valueOf(3.8), 15, 3, 2);

        entityManager.persist(agg1);
        entityManager.persist(agg2);
        entityManager.persist(agg3);
        entityManager.flush();

        // When
        Object[] totals = dashboardAggregateRepository.calculateTotalsForSource(
                reviewSource.getId(), twoDaysAgo, today);

        // Then
        assertThat(totals).isNotNull();
        assertThat(totals).hasSize(1); // Single row result
        // Extract the row (which contains 4 values)
        Object[] row = (Object[]) totals[0];
        assertThat(row[0]).isEqualTo(100L); // Total reviews: 50 + 30 + 20
        assertThat(row[1]).isEqualTo(75L); // Positive: 40 + 20 + 15
        assertThat(row[2]).isEqualTo(13L); // Negative: 5 + 5 + 3
        assertThat(row[3]).isEqualTo(12L); // Neutral: 5 + 5 + 2
    }

    @Test
    @DisplayName("Should calculate totals for brand (all sources)")
    void shouldCalculateTotalsForBrand() {
        // Given - Create second review source
        ReviewSource source2 = ReviewSource.builder()
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page/456")
                .externalProfileId("fb-456")
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source2);

        LocalDate today = LocalDate.now();

        DashboardAggregate agg1 = createAggregate(reviewSource, today, 50, BigDecimal.valueOf(4.5), 40, 5, 5);
        DashboardAggregate agg2 = createAggregate(source2, today, 30, BigDecimal.valueOf(4.0), 20, 5, 5);

        entityManager.persist(agg1);
        entityManager.persist(agg2);
        entityManager.flush();

        // When
        Object[] totals = dashboardAggregateRepository.calculateTotalsForBrand(
                brand.getId(), today, today);

        // Then
        assertThat(totals).isNotNull();
        assertThat(totals).hasSize(1); // Single row result
        // Extract the row (which contains 4 values)
        Object[] row = (Object[]) totals[0];
        assertThat(row[0]).isEqualTo(80L); // Total: 50 + 30
        assertThat(row[1]).isEqualTo(60L); // Positive: 40 + 20
        assertThat(row[2]).isEqualTo(10L); // Negative: 5 + 5
        assertThat(row[3]).isEqualTo(10L); // Neutral: 5 + 5
    }

    @Test
    @DisplayName("Should calculate weighted average rating")
    void shouldCalculateWeightedAverageRating() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Day 1: 100 reviews, avg 4.5
        DashboardAggregate agg1 = createAggregate(today, 100, BigDecimal.valueOf(4.5), 80, 10, 10);

        // Day 2: 50 reviews, avg 3.0
        DashboardAggregate agg2 = createAggregate(yesterday, 50, BigDecimal.valueOf(3.0), 20, 20, 10);

        entityManager.persist(agg1);
        entityManager.persist(agg2);
        entityManager.flush();

        // When
        Double weightedAvg = dashboardAggregateRepository.calculateWeightedAverageRating(
                reviewSource.getId(), yesterday, today);

        // Then
        // (4.5 * 100 + 3.0 * 50) / 150 = (450 + 150) / 150 = 4.0
        assertThat(weightedAvg).isNotNull();
        assertThat(weightedAvg).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Should find aggregates ordered by date")
    void shouldFindByReviewSourceIdOrderByDate() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        DashboardAggregate agg1 = createAggregate(today, 50, BigDecimal.valueOf(4.5), 40, 5, 5);
        DashboardAggregate agg2 = createAggregate(twoDaysAgo, 20, BigDecimal.valueOf(3.8), 15, 3, 2);
        DashboardAggregate agg3 = createAggregate(yesterday, 30, BigDecimal.valueOf(4.0), 20, 5, 5);

        entityManager.persist(agg1);
        entityManager.persist(agg2);
        entityManager.persist(agg3);
        entityManager.flush();
        entityManager.clear();

        // When
        List<DashboardAggregate> aggregates = dashboardAggregateRepository
                .findByReviewSourceIdOrderByDate(reviewSource.getId());

        // Then
        assertThat(aggregates).hasSize(3);
        assertThat(aggregates).extracting(DashboardAggregate::getTotalReviews)
                .containsExactly(20, 30, 50); // Ordered by date ASC
    }

    @Test
    @DisplayName("Should check if aggregate exists for source and date")
    void shouldCheckExistsByReviewSourceIdAndDate() {
        // Given
        LocalDate today = LocalDate.now();
        DashboardAggregate aggregate = createAggregate(today, 50, BigDecimal.valueOf(4.0), 30, 10, 10);
        entityManager.persist(aggregate);
        entityManager.flush();

        // When & Then
        assertThat(dashboardAggregateRepository.existsByReviewSourceIdAndDate(
                reviewSource.getId(), today)).isTrue();

        assertThat(dashboardAggregateRepository.existsByReviewSourceIdAndDate(
                reviewSource.getId(), today.minusDays(1))).isFalse();
    }

    @Test
    @DisplayName("Should count aggregates for review source")
    void shouldCountByReviewSourceId() {
        // Given
        for (int i = 0; i < 5; i++) {
            DashboardAggregate agg = createAggregate(LocalDate.now().minusDays(i),
                    50, BigDecimal.valueOf(4.0), 30, 10, 10);
            entityManager.persist(agg);
        }
        entityManager.flush();

        // When
        long count = dashboardAggregateRepository.countByReviewSourceId(reviewSource.getId());

        // Then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("Should validate percentage calculations")
    void shouldCalculatePercentages() {
        // Given
        DashboardAggregate aggregate = createAggregate(LocalDate.now(), 100, BigDecimal.valueOf(4.0),
                70, 20, 10);

        // When & Then
        assertThat(aggregate.getPositivePercentage()).isEqualTo(70.0);
        assertThat(aggregate.getNegativePercentage()).isEqualTo(20.0);
        assertThat(aggregate.getNeutralPercentage()).isEqualTo(10.0);

        // Edge case: zero reviews
        DashboardAggregate emptyAggregate = createAggregate(LocalDate.now(), 0, null, 0, 0, 0);
        assertThat(emptyAggregate.getPositivePercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should validate needs recalculation logic")
    void shouldCheckNeedsRecalculation() {
        // Given
        Instant now = Instant.now();
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        Instant thirtyMinutesAgo = now.minus(30, ChronoUnit.MINUTES);

        DashboardAggregate staleAggregate = createAggregate(LocalDate.now(), 50, BigDecimal.valueOf(4.0),
                30, 10, 10);
        staleAggregate.setLastCalculatedAt(twoHoursAgo);

        DashboardAggregate freshAggregate = createAggregate(LocalDate.now(), 50, BigDecimal.valueOf(4.0),
                30, 10, 10);
        freshAggregate.setLastCalculatedAt(thirtyMinutesAgo);

        // When & Then
        assertThat(staleAggregate.needsRecalculation()).isTrue();
        assertThat(freshAggregate.needsRecalculation()).isFalse();
    }

    // ===== Helper Methods =====

    private DashboardAggregate createAggregate(LocalDate date, Integer totalReviews, BigDecimal avgRating,
                                               Integer positiveCount, Integer negativeCount, Integer neutralCount) {
        return createAggregate(reviewSource, date, totalReviews, avgRating, positiveCount, negativeCount, neutralCount);
    }

    private DashboardAggregate createAggregate(ReviewSource source, LocalDate date, Integer totalReviews,
                                               BigDecimal avgRating, Integer positiveCount,
                                               Integer negativeCount, Integer neutralCount) {
        return DashboardAggregate.builder()
                .reviewSource(source)
                .date(date)
                .totalReviews(totalReviews)
                .avgRating(avgRating)
                .positiveCount(positiveCount)
                .negativeCount(negativeCount)
                .neutralCount(neutralCount)
                .lastCalculatedAt(Instant.now())
                .build();
    }
}
