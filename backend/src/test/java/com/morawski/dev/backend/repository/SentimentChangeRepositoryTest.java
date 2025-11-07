package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.AbstractIntegrationTest;
import com.morawski.dev.backend.PostgreSQLIntegrationTest;
import com.morawski.dev.backend.dto.common.*;
import com.morawski.dev.backend.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SentimentChangeRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-007: Manual Sentiment Correction
 * - AI accuracy metrics and analytics
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("SentimentChangeRepository Integration Tests (PostgreSQL)")
class SentimentChangeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SentimentChangeRepository sentimentChangeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Brand brand;
    private ReviewSource reviewSource;
    private Review review1;
    private Review review2;

    @BeforeEach
    void setUp() {
        // Create test data hierarchy: User -> Brand -> ReviewSource -> Reviews
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

        review1 = createReview("ext-001", "Great service!", (short) 5, Sentiment.POSITIVE);
        entityManager.persist(review1);

        review2 = createReview("ext-002", "Terrible experience", (short) 1, Sentiment.NEGATIVE);
        entityManager.persist(review2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should save and retrieve sentiment change by ID")
    void shouldSaveAndRetrieveSentimentChange() {
        // Given
        SentimentChange change = SentimentChange.builder()
                .review(review1)
                .oldSentiment(Sentiment.NEUTRAL)
                .newSentiment(Sentiment.POSITIVE)
                .changeReason(ChangeReason.AI_INITIAL)
                .changedByUser(null)
                .build();

        // When
        SentimentChange savedChange = sentimentChangeRepository.save(change);
        entityManager.flush();
        entityManager.clear();

        // Then
        SentimentChange foundChange = sentimentChangeRepository.findById(savedChange.getId()).orElseThrow();
        assertThat(foundChange.getOldSentiment()).isEqualTo(Sentiment.NEUTRAL);
        assertThat(foundChange.getNewSentiment()).isEqualTo(Sentiment.POSITIVE);
        assertThat(foundChange.getChangeReason()).isEqualTo(ChangeReason.AI_INITIAL);
        assertThat(foundChange.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find all sentiment changes for a review ordered by date")
    void shouldFindByReviewIdOrderByChangedAtDesc() {
        // Given
        Instant now = Instant.now();

        SentimentChange change1 = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.AI_INITIAL, null, now.minus(2, ChronoUnit.HOURS));
        SentimentChange change2 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, now.minus(1, ChronoUnit.HOURS));
        SentimentChange change3 = createChange(review1, Sentiment.NEGATIVE, Sentiment.NEUTRAL,
                ChangeReason.USER_CORRECTION, user, now);

        entityManager.persist(change1);
        entityManager.persist(change2);
        entityManager.persist(change3);
        entityManager.flush();
        entityManager.clear();

        // When
        List<SentimentChange> changes = sentimentChangeRepository.findByReviewIdOrderByChangedAtDesc(review1.getId());

        // Then
        assertThat(changes).hasSize(3);
        assertThat(changes.get(0).getNewSentiment()).isEqualTo(Sentiment.NEUTRAL); // Most recent
        assertThat(changes.get(1).getNewSentiment()).isEqualTo(Sentiment.NEGATIVE);
        assertThat(changes.get(2).getNewSentiment()).isEqualTo(Sentiment.POSITIVE); // Oldest
    }

    @Test
    @DisplayName("Should find sentiment changes by change reason")
    void shouldFindByChangeReason() {
        // Given
        SentimentChange aiChange = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.AI_INITIAL, null, Instant.now());
        SentimentChange userChange = createChange(review2, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange reanalysisChange = createChange(review1, Sentiment.POSITIVE, Sentiment.NEUTRAL,
                ChangeReason.AI_REANALYSIS, null, Instant.now());

        entityManager.persist(aiChange);
        entityManager.persist(userChange);
        entityManager.persist(reanalysisChange);
        entityManager.flush();

        // When
        List<SentimentChange> aiInitial = sentimentChangeRepository.findByChangeReason(ChangeReason.AI_INITIAL);
        List<SentimentChange> userCorrections = sentimentChangeRepository.findByChangeReason(ChangeReason.USER_CORRECTION);
        List<SentimentChange> reanalysis = sentimentChangeRepository.findByChangeReason(ChangeReason.AI_REANALYSIS);

        // Then
        assertThat(aiInitial).hasSize(1);
        assertThat(userCorrections).hasSize(1);
        assertThat(reanalysis).hasSize(1);
    }

    @Test
    @DisplayName("Should count sentiment changes by change reason")
    void shouldCountByChangeReason() {
        // Given - Create 10 AI initial classifications and 2 user corrections
        for (int i = 0; i < 10; i++) {
            Review review = createReview("ext-" + i, "Review " + i, (short) 4, Sentiment.POSITIVE);
            entityManager.persist(review);

            SentimentChange aiChange = createChange(review, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                    ChangeReason.AI_INITIAL, null, Instant.now());
            entityManager.persist(aiChange);
        }

        SentimentChange userChange1 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange userChange2 = createChange(review2, Sentiment.NEGATIVE, Sentiment.NEUTRAL,
                ChangeReason.USER_CORRECTION, user, Instant.now());

        entityManager.persist(userChange1);
        entityManager.persist(userChange2);
        entityManager.flush();

        // When
        long aiCount = sentimentChangeRepository.countByChangeReason(ChangeReason.AI_INITIAL);
        long userCount = sentimentChangeRepository.countByChangeReason(ChangeReason.USER_CORRECTION);

        // Then
        assertThat(aiCount).isEqualTo(10);
        assertThat(userCount).isEqualTo(2);

        // AI accuracy should be 80% (2 corrections out of 10 AI classifications)
        double accuracy = (1.0 - (userCount * 1.0 / aiCount)) * 100.0;
        assertThat(accuracy).isEqualTo(80.0);
    }

    @Test
    @DisplayName("Should find user corrections for a review source")
    void shouldFindUserCorrectionsByReviewSourceId() {
        // Given
        SentimentChange aiChange = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.AI_INITIAL, null, Instant.now());
        SentimentChange userChange1 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange userChange2 = createChange(review2, Sentiment.NEGATIVE, Sentiment.NEUTRAL,
                ChangeReason.USER_CORRECTION, user, Instant.now());

        entityManager.persist(aiChange);
        entityManager.persist(userChange1);
        entityManager.persist(userChange2);
        entityManager.flush();

        // When
        List<SentimentChange> userCorrections = sentimentChangeRepository
                .findUserCorrectionsByReviewSourceId(reviewSource.getId());

        // Then
        assertThat(userCorrections).hasSize(2);
        assertThat(userCorrections).allMatch(SentimentChange::isUserChange);
    }

    @Test
    @DisplayName("Should find user corrections by user ID")
    void shouldFindUserCorrectionsByUserId() {
        // Given
        User otherUser = User.builder()
                .email("other@example.com")
                .passwordHash("hash456")
                .planType(PlanType.PREMIUM)
                .maxSourcesAllowed(10)
                .emailVerified(true)
                .build();
        entityManager.persist(otherUser);

        SentimentChange userChange1 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange userChange2 = createChange(review2, Sentiment.NEGATIVE, Sentiment.NEUTRAL,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange otherUserChange = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.USER_CORRECTION, otherUser, Instant.now());

        entityManager.persist(userChange1);
        entityManager.persist(userChange2);
        entityManager.persist(otherUserChange);
        entityManager.flush();

        // When
        List<SentimentChange> userCorrections = sentimentChangeRepository.findUserCorrectionsByUserId(user.getId());
        List<SentimentChange> otherCorrections = sentimentChangeRepository.findUserCorrectionsByUserId(otherUser.getId());

        // Then
        assertThat(userCorrections).hasSize(2);
        assertThat(otherCorrections).hasSize(1);
    }

    @Test
    @DisplayName("Should find sentiment changes within date range")
    void shouldFindByChangedAtBetween() {
        // Given
        Instant now = Instant.now();
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);
        Instant tenDaysAgo = now.minus(10, ChronoUnit.DAYS);

        SentimentChange recentChange = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.USER_CORRECTION, user, twoDaysAgo);
        SentimentChange oldChange = createChange(review2, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, tenDaysAgo);

        entityManager.persist(recentChange);
        entityManager.persist(oldChange);
        entityManager.flush();

        // When - query for changes in last 7 days
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        List<SentimentChange> recentChanges = sentimentChangeRepository.findByChangedAtBetween(
                sevenDaysAgo, now);

        // Then
        assertThat(recentChanges).hasSize(1);
        assertThat(recentChanges.get(0).getChangedAt()).isAfter(sevenDaysAgo);
    }

    @Test
    @DisplayName("Should calculate AI accuracy for a review source")
    void shouldCalculateAIAccuracyForSource() {
        // Given - 8 AI classifications, 2 user corrections = 75% accuracy
        for (int i = 0; i < 8; i++) {
            Review review = createReview("ext-ai-" + i, "Review " + i, (short) 4, Sentiment.POSITIVE);
            entityManager.persist(review);

            SentimentChange aiChange = createChange(review, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                    ChangeReason.AI_INITIAL, null, Instant.now());
            entityManager.persist(aiChange);
        }

        // User corrections
        SentimentChange correction1 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange correction2 = createChange(review2, Sentiment.NEGATIVE, Sentiment.NEUTRAL,
                ChangeReason.USER_CORRECTION, user, Instant.now());

        entityManager.persist(correction1);
        entityManager.persist(correction2);
        entityManager.flush();

        // When
        Double accuracy = sentimentChangeRepository.calculateAIAccuracyForSource(reviewSource.getId());

        // Then
        // (1 - 2/8) * 100 = 75%
        assertThat(accuracy).isNotNull();
        assertThat(accuracy).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Should calculate global AI accuracy")
    void shouldCalculateGlobalAIAccuracy() {
        // Given - 10 AI classifications, 3 user corrections = 70% accuracy
        for (int i = 0; i < 10; i++) {
            Review review = createReview("ext-global-" + i, "Review " + i, (short) 4, Sentiment.POSITIVE);
            entityManager.persist(review);

            SentimentChange aiChange = createChange(review, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                    ChangeReason.AI_INITIAL, null, Instant.now());
            entityManager.persist(aiChange);
        }

        for (int i = 0; i < 3; i++) {
            Review review = createReview("ext-correction-" + i, "Review " + i, (short) 2, Sentiment.NEGATIVE);
            entityManager.persist(review);

            SentimentChange correction = createChange(review, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                    ChangeReason.USER_CORRECTION, user, Instant.now());
            entityManager.persist(correction);
        }

        entityManager.flush();

        // When
        Double accuracy = sentimentChangeRepository.calculateGlobalAIAccuracy();

        // Then
        // (1 - 3/10) * 100 = 70%
        assertThat(accuracy).isNotNull();
        assertThat(accuracy).isEqualTo(70.0);
    }

    @Test
    @DisplayName("Should find correction patterns (old -> new sentiment)")
    void shouldFindCorrectionPatternsByReviewSourceId() {
        // Given - Create correction patterns
        // POSITIVE -> NEGATIVE: 3 times
        for (int i = 0; i < 3; i++) {
            Review review = createReview("ext-pn-" + i, "Review " + i, (short) 4, Sentiment.POSITIVE);
            entityManager.persist(review);

            SentimentChange change = createChange(review, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                    ChangeReason.USER_CORRECTION, user, Instant.now());
            entityManager.persist(change);
        }

        // NEUTRAL -> POSITIVE: 2 times
        for (int i = 0; i < 2; i++) {
            Review review = createReview("ext-np-" + i, "Review " + i, (short) 3, Sentiment.NEUTRAL);
            entityManager.persist(review);

            SentimentChange change = createChange(review, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                    ChangeReason.USER_CORRECTION, user, Instant.now());
            entityManager.persist(change);
        }

        entityManager.flush();

        // When
        List<Object[]> patterns = sentimentChangeRepository.findCorrectionPatternsByReviewSourceId(
                reviewSource.getId());

        // Then
        assertThat(patterns).hasSize(2);

        // Most common pattern should be first (POSITIVE -> NEGATIVE: 3)
        Object[] topPattern = patterns.get(0);
        assertThat(topPattern[0]).isEqualTo(Sentiment.POSITIVE);
        assertThat(topPattern[1]).isEqualTo(Sentiment.NEGATIVE);
        assertThat(topPattern[2]).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should find reviews with multiple corrections")
    void shouldFindReviewsWithMultipleCorrections() {
        // Given - Review with 3 corrections
        SentimentChange change1 = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.AI_INITIAL, null, Instant.now().minus(3, ChronoUnit.HOURS));
        SentimentChange change2 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now().minus(2, ChronoUnit.HOURS));
        SentimentChange change3 = createChange(review1, Sentiment.NEGATIVE, Sentiment.NEUTRAL,
                ChangeReason.USER_CORRECTION, user, Instant.now());

        entityManager.persist(change1);
        entityManager.persist(change2);
        entityManager.persist(change3);
        entityManager.flush();

        // When
        List<Object[]> multipleCorrections = sentimentChangeRepository.findReviewsWithMultipleCorrections(
                reviewSource.getId(), 2);

        // Then
        assertThat(multipleCorrections).hasSize(1);
        Object[] result = multipleCorrections.get(0);
        assertThat(result[0]).isEqualTo(review1.getId());
        assertThat(result[1]).isEqualTo(3L); // 3 changes
    }

    @Test
    @DisplayName("Should find changes by old and new sentiment")
    void shouldFindByOldSentimentAndNewSentiment() {
        // Given
        SentimentChange change1 = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange change2 = createChange(review2, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange change3 = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());

        entityManager.persist(change1);
        entityManager.persist(change2);
        entityManager.persist(change3);
        entityManager.flush();

        // When
        List<SentimentChange> posToNeg = sentimentChangeRepository.findByOldSentimentAndNewSentiment(
                Sentiment.POSITIVE, Sentiment.NEGATIVE);
        List<SentimentChange> neutralToPos = sentimentChangeRepository.findByOldSentimentAndNewSentiment(
                Sentiment.NEUTRAL, Sentiment.POSITIVE);

        // Then
        assertThat(posToNeg).hasSize(2);
        assertThat(neutralToPos).hasSize(1);
    }

    @Test
    @DisplayName("Should count total sentiment changes for a review source")
    void shouldCountByReviewSourceId() {
        // Given
        SentimentChange change1 = createChange(review1, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.AI_INITIAL, null, Instant.now());
        SentimentChange change2 = createChange(review2, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());

        entityManager.persist(change1);
        entityManager.persist(change2);
        entityManager.flush();

        // When
        long count = sentimentChangeRepository.countByReviewSourceId(reviewSource.getId());

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should validate helper methods in SentimentChange entity")
    void shouldValidateSentimentChangeHelperMethods() {
        // Given
        SentimentChange userChange = createChange(review1, Sentiment.POSITIVE, Sentiment.NEGATIVE,
                ChangeReason.USER_CORRECTION, user, Instant.now());
        SentimentChange aiChange = createChange(review2, Sentiment.NEUTRAL, Sentiment.POSITIVE,
                ChangeReason.AI_INITIAL, null, Instant.now());
        SentimentChange noChange = createChange(review1, Sentiment.POSITIVE, Sentiment.POSITIVE,
                ChangeReason.AI_REANALYSIS, null, Instant.now());

        // When & Then
        assertThat(userChange.isUserChange()).isTrue();
        assertThat(aiChange.isUserChange()).isFalse();

        assertThat(userChange.isSentimentDifferent()).isTrue();
        assertThat(noChange.isSentimentDifferent()).isFalse();
    }

    // ===== Helper Methods =====

    private Review createReview(String externalId, String content, Short rating, Sentiment sentiment) {
        return Review.builder()
                .externalReviewId(externalId)
                .content(content)
                .contentHash("hash-" + externalId)
                .authorName("Test Author")
                .rating(rating)
                .sentiment(sentiment)
                .sentimentConfidence(BigDecimal.valueOf(0.95))
                .publishedAt(Instant.now())
                .fetchedAt(Instant.now())
                .reviewSource(reviewSource)
                .build();
    }

    private SentimentChange createChange(Review review, Sentiment oldSentiment, Sentiment newSentiment,
                                         ChangeReason reason, User changedBy, Instant changedAt) {
        return SentimentChange.builder()
                .review(review)
                .oldSentiment(oldSentiment)
                .newSentiment(newSentiment)
                .changeReason(reason)
                .changedByUser(changedBy)
                .changedAt(changedAt)
                .build();
    }
}
