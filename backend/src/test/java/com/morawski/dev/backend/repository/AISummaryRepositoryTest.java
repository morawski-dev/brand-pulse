package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.AbstractIntegrationTest;
import com.morawski.dev.backend.PostgreSQLIntegrationTest;
import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.entity.AISummary;
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
 * Integration tests for {@link AISummaryRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-004: Viewing Aggregated Reviews (AI text summary)
 * - AI summary caching and expiration
 * - Cost tracking and analytics
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("AISummaryRepository Integration Tests (PostgreSQL)")
class AISummaryRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AISummaryRepository aiSummaryRepository;

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
    @DisplayName("Should save and retrieve AI summary by ID")
    void shouldSaveAndRetrieveAISummary() {
        // Given
        AISummary summary = createSummary(
                "75% positive reviews; customers praise speed but complain about prices",
                "anthropic/claude-3-haiku",
                1500,
                Instant.now().plus(24, ChronoUnit.HOURS)
        );

        // When
        AISummary saved = aiSummaryRepository.save(summary);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<AISummary> found = aiSummaryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSummaryText()).contains("75% positive reviews");
        assertThat(found.get().getModelUsed()).isEqualTo("anthropic/claude-3-haiku");
        assertThat(found.get().getTokenCount()).isEqualTo(1500);
        assertThat(found.get().getValidUntil()).isNotNull();
    }

    @Test
    @DisplayName("Should find valid (not expired) summary for review source")
    void shouldFindValidSummaryByReviewSourceId() {
        // Given
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(24, ChronoUnit.HOURS);
        Instant pastExpiry = now.minus(1, ChronoUnit.HOURS);

        AISummary validSummary = createSummary("Valid summary", "model-1", 1000, futureExpiry);
        AISummary expiredSummary = createSummary("Expired summary", "model-1", 1000, pastExpiry);

        entityManager.persist(validSummary);
        entityManager.persist(expiredSummary);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<AISummary> found = aiSummaryRepository.findValidSummaryByReviewSourceId(
                reviewSource.getId(), now);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSummaryText()).isEqualTo("Valid summary");
        assertThat(found.get().isValid()).isTrue();
    }

    @Test
    @DisplayName("Should find latest summary regardless of validity")
    void shouldFindLatestSummaryByReviewSourceId() {
        // Given
        Instant now = Instant.now();

        AISummary oldSummary = createSummary("Old summary", "model-1", 1000,
                now.plus(24, ChronoUnit.HOURS));
        oldSummary.setGeneratedAt(now.minus(2, ChronoUnit.DAYS));

        AISummary latestSummary = createSummary("Latest summary", "model-2", 1500,
                now.minus(1, ChronoUnit.HOURS)); // Expired but most recent
        latestSummary.setGeneratedAt(now.minus(1, ChronoUnit.HOURS));

        entityManager.persist(oldSummary);
        entityManager.persist(latestSummary);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<AISummary> found = aiSummaryRepository.findLatestSummaryByReviewSourceId(
                reviewSource.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSummaryText()).isEqualTo("Latest summary");
    }

    @Test
    @DisplayName("Should find all summaries for review source ordered by generation date")
    void shouldFindByReviewSourceIdOrderByGeneratedAtDesc() {
        // Given
        Instant now = Instant.now();

        AISummary summary1 = createSummary("Summary 1", "model-1", 1000, now.plus(24, ChronoUnit.HOURS));
        summary1.setGeneratedAt(now.minus(3, ChronoUnit.DAYS));

        AISummary summary2 = createSummary("Summary 2", "model-1", 1200, now.plus(24, ChronoUnit.HOURS));
        summary2.setGeneratedAt(now.minus(2, ChronoUnit.DAYS));

        AISummary summary3 = createSummary("Summary 3", "model-2", 1500, now.plus(24, ChronoUnit.HOURS));
        summary3.setGeneratedAt(now.minus(1, ChronoUnit.DAYS));

        entityManager.persist(summary1);
        entityManager.persist(summary2);
        entityManager.persist(summary3);
        entityManager.flush();
        entityManager.clear();

        // When
        List<AISummary> summaries = aiSummaryRepository.findByReviewSourceIdOrderByGeneratedAtDesc(
                reviewSource.getId());

        // Then
        assertThat(summaries).hasSize(3);
        assertThat(summaries).extracting(AISummary::getSummaryText)
                .containsExactly("Summary 3", "Summary 2", "Summary 1"); // Newest first
    }

    @Test
    @DisplayName("Should find expired summaries")
    void shouldFindExpiredSummaries() {
        // Given
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(24, ChronoUnit.HOURS);
        Instant pastExpiry = now.minus(1, ChronoUnit.HOURS);

        AISummary validSummary = createSummary("Valid", "model-1", 1000, futureExpiry);
        AISummary expiredSummary1 = createSummary("Expired 1", "model-1", 1000, pastExpiry);
        AISummary expiredSummary2 = createSummary("Expired 2", "model-1", 1000,
                now.minus(5, ChronoUnit.HOURS));

        entityManager.persist(validSummary);
        entityManager.persist(expiredSummary1);
        entityManager.persist(expiredSummary2);
        entityManager.flush();

        // When
        List<AISummary> expiredSummaries = aiSummaryRepository.findExpiredSummaries(now);

        // Then
        assertThat(expiredSummaries).hasSize(2);
        assertThat(expiredSummaries).extracting(AISummary::getSummaryText)
                .containsExactlyInAnyOrder("Expired 1", "Expired 2");
    }

    @Test
    @DisplayName("Should find summaries generated within date range")
    void shouldFindByGeneratedAtBetween() {
        // Given
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        Instant tenDaysAgo = now.minus(10, ChronoUnit.DAYS);

        AISummary recentSummary = createSummary("Recent", "model-1", 1000, now.plus(24, ChronoUnit.HOURS));
        recentSummary.setGeneratedAt(threeDaysAgo);

        AISummary oldSummary = createSummary("Old", "model-1", 1000, now.plus(24, ChronoUnit.HOURS));
        oldSummary.setGeneratedAt(tenDaysAgo);

        entityManager.persist(recentSummary);
        entityManager.persist(oldSummary);
        entityManager.flush();

        // When - query for summaries in last 7 days
        List<AISummary> summaries = aiSummaryRepository.findByGeneratedAtBetween(sevenDaysAgo, now);

        // Then
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getSummaryText()).isEqualTo("Recent");
    }

    @Test
    @DisplayName("Should find summaries by AI model")
    void shouldFindByModelUsed() {
        // Given
        AISummary claudeSummary1 = createSummary("Claude summary 1", "anthropic/claude-3-haiku", 1000,
                Instant.now().plus(24, ChronoUnit.HOURS));
        AISummary claudeSummary2 = createSummary("Claude summary 2", "anthropic/claude-3-haiku", 1200,
                Instant.now().plus(24, ChronoUnit.HOURS));
        AISummary gptSummary = createSummary("GPT summary", "openai/gpt-4", 2000,
                Instant.now().plus(24, ChronoUnit.HOURS));

        entityManager.persist(claudeSummary1);
        entityManager.persist(claudeSummary2);
        entityManager.persist(gptSummary);
        entityManager.flush();

        // When
        List<AISummary> claudeSummaries = aiSummaryRepository.findByModelUsed("anthropic/claude-3-haiku");
        List<AISummary> gptSummaries = aiSummaryRepository.findByModelUsed("openai/gpt-4");

        // Then
        assertThat(claudeSummaries).hasSize(2);
        assertThat(gptSummaries).hasSize(1);
    }

    @Test
    @DisplayName("Should calculate total token count for review source")
    void shouldCalculateTotalTokensByReviewSourceId() {
        // Given
        AISummary summary1 = createSummary("Summary 1", "model-1", 1000, Instant.now().plus(24, ChronoUnit.HOURS));
        AISummary summary2 = createSummary("Summary 2", "model-1", 1500, Instant.now().plus(24, ChronoUnit.HOURS));
        AISummary summary3 = createSummary("Summary 3", "model-1", 2000, Instant.now().plus(24, ChronoUnit.HOURS));

        entityManager.persist(summary1);
        entityManager.persist(summary2);
        entityManager.persist(summary3);
        entityManager.flush();

        // When
        Long totalTokens = aiSummaryRepository.calculateTotalTokensByReviewSourceId(reviewSource.getId());

        // Then
        assertThat(totalTokens).isEqualTo(4500L); // 1000 + 1500 + 2000
    }

    @Test
    @DisplayName("Should calculate total tokens for date range")
    void shouldCalculateTotalTokensByDateRange() {
        // Given
        Instant now = Instant.now();
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

        AISummary recentSummary = createSummary("Recent", "model-1", 1000, now.plus(24, ChronoUnit.HOURS));
        recentSummary.setGeneratedAt(twoDaysAgo);

        AISummary oldSummary = createSummary("Old", "model-1", 2000, now.plus(24, ChronoUnit.HOURS));
        oldSummary.setGeneratedAt(fiveDaysAgo);

        entityManager.persist(recentSummary);
        entityManager.persist(oldSummary);
        entityManager.flush();

        // When - calculate tokens for last 3 days
        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        Long totalTokens = aiSummaryRepository.calculateTotalTokensByDateRange(threeDaysAgo, now);

        // Then
        assertThat(totalTokens).isEqualTo(1000L); // Only recent summary
    }

    @Test
    @DisplayName("Should calculate average token count per model")
    void shouldCalculateAverageTokensByModel() {
        // Given
        AISummary summary1 = createSummary("Summary 1", "anthropic/claude-3-haiku", 1000,
                Instant.now().plus(24, ChronoUnit.HOURS));
        AISummary summary2 = createSummary("Summary 2", "anthropic/claude-3-haiku", 1500,
                Instant.now().plus(24, ChronoUnit.HOURS));
        AISummary summary3 = createSummary("Summary 3", "anthropic/claude-3-haiku", 2000,
                Instant.now().plus(24, ChronoUnit.HOURS));

        entityManager.persist(summary1);
        entityManager.persist(summary2);
        entityManager.persist(summary3);
        entityManager.flush();

        // When
        Double avgTokens = aiSummaryRepository.calculateAverageTokensByModel("anthropic/claude-3-haiku");

        // Then
        assertThat(avgTokens).isEqualTo(1500.0); // (1000 + 1500 + 2000) / 3
    }

    @Test
    @DisplayName("Should count summaries for review source")
    void shouldCountByReviewSourceId() {
        // Given
        for (int i = 0; i < 5; i++) {
            AISummary summary = createSummary("Summary " + i, "model-1", 1000,
                    Instant.now().plus(24, ChronoUnit.HOURS));
            entityManager.persist(summary);
        }
        entityManager.flush();

        // When
        long count = aiSummaryRepository.countByReviewSourceId(reviewSource.getId());

        // Then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("Should check if valid summary exists")
    void shouldCheckHasValidSummary() {
        // Given
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(24, ChronoUnit.HOURS);

        AISummary validSummary = createSummary("Valid", "model-1", 1000, futureExpiry);
        entityManager.persist(validSummary);
        entityManager.flush();

        // When & Then
        assertThat(aiSummaryRepository.hasValidSummary(reviewSource.getId(), now)).isTrue();

        // After expiry
        Instant afterExpiry = futureExpiry.plus(1, ChronoUnit.HOURS);
        assertThat(aiSummaryRepository.hasValidSummary(reviewSource.getId(), afterExpiry)).isFalse();
    }

    @Test
    @DisplayName("Should find summaries for brand (across all sources)")
    void shouldFindByBrandId() {
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

        AISummary summary1 = createSummary("Source 1 summary", "model-1", 1000,
                Instant.now().plus(24, ChronoUnit.HOURS));
        summary1.setReviewSource(reviewSource);

        AISummary summary2 = createSummary("Source 2 summary", "model-1", 1200,
                Instant.now().plus(24, ChronoUnit.HOURS));
        summary2.setReviewSource(source2);

        entityManager.persist(summary1);
        entityManager.persist(summary2);
        entityManager.flush();
        entityManager.clear();

        // When
        List<AISummary> brandSummaries = aiSummaryRepository.findByBrandId(brand.getId());

        // Then
        assertThat(brandSummaries).hasSize(2);
    }

    @Test
    @DisplayName("Should find summaries expiring soon")
    void shouldFindSummariesExpiringSoon() {
        // Given
        Instant now = Instant.now();
        Instant inOneHour = now.plus(1, ChronoUnit.HOURS);
        Instant inFiveHours = now.plus(5, ChronoUnit.HOURS);
        Instant inTenHours = now.plus(10, ChronoUnit.HOURS);

        AISummary expiringSoon1 = createSummary("Expiring soon 1", "model-1", 1000, inOneHour);
        AISummary expiringSoon2 = createSummary("Expiring soon 2", "model-1", 1000, inFiveHours);
        AISummary notExpiringSoon = createSummary("Not expiring soon", "model-1", 1000, inTenHours);

        entityManager.persist(expiringSoon1);
        entityManager.persist(expiringSoon2);
        entityManager.persist(notExpiringSoon);
        entityManager.flush();

        // When - find summaries expiring within next 6 hours
        Instant sixHoursFromNow = now.plus(6, ChronoUnit.HOURS);
        List<AISummary> expiringSoon = aiSummaryRepository.findSummariesExpiringSoon(now, sixHoursFromNow);

        // Then
        assertThat(expiringSoon).hasSize(2);
        assertThat(expiringSoon).extracting(AISummary::getSummaryText)
                .containsExactlyInAnyOrder("Expiring soon 1", "Expiring soon 2");
    }

    @Test
    @DisplayName("Should validate summary validity checks")
    void shouldValidateSummaryValidity() {
        // Given
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(24, ChronoUnit.HOURS);
        Instant pastExpiry = now.minus(1, ChronoUnit.HOURS);

        AISummary validSummary = createSummary("Valid", "model-1", 1000, futureExpiry);
        AISummary expiredSummary = createSummary("Expired", "model-1", 1000, pastExpiry);
        AISummary noExpirySummary = createSummary("No expiry", "model-1", 1000, null);

        // When & Then
        assertThat(validSummary.isValid()).isTrue();
        assertThat(validSummary.isExpired()).isFalse();

        assertThat(expiredSummary.isValid()).isFalse();
        assertThat(expiredSummary.isExpired()).isTrue();

        assertThat(noExpirySummary.isValid()).isTrue(); // null validUntil means always valid
        assertThat(noExpirySummary.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should set validity period correctly")
    void shouldSetValidityPeriod() {
        // Given
        AISummary summary = createSummary("Test", "model-1", 1000, null);

        // When
        summary.setValidityPeriod(24); // 24 hours

        // Then
        assertThat(summary.getValidUntil()).isNotNull();
        assertThat(summary.getValidUntil()).isAfter(Instant.now());
        assertThat(summary.getValidUntil()).isBefore(Instant.now().plus(25, ChronoUnit.HOURS));
    }

    // ===== Helper Methods =====

    private AISummary createSummary(String summaryText, String modelUsed, Integer tokenCount, Instant validUntil) {
        return AISummary.builder()
                .reviewSource(reviewSource)
                .summaryText(summaryText)
                .modelUsed(modelUsed)
                .tokenCount(tokenCount)
                .generatedAt(Instant.now())
                .validUntil(validUntil)
                .build();
    }
}
