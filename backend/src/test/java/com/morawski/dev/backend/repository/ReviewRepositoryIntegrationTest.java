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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ReviewRepository}.
 * Uses Testcontainers with PostgreSQL for production-like testing.
 *
 * Tests cover:
 * - US-004: Viewing Aggregated Reviews
 * - US-005: Filtering Negative Reviews
 * - US-006: Switching Between Locations
 * - US-007: Manual Sentiment Correction
 *
 * @PostgreSQLIntegrationTest provides:
 * - PostgreSQL container via Testcontainers (from AbstractIntegrationTest)
 * - Transaction rollback after each test
 * - TestEntityManager for test data setup
 */
@PostgreSQLIntegrationTest
@DisplayName("ReviewRepository Integration Tests (PostgreSQL)")
class ReviewRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ReviewSource reviewSource;
    private Brand brand;
    private User user;

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
    @DisplayName("Should save and retrieve review with all fields")
    void shouldSaveAndRetrieveReview() {
        // Given
        Review review = createReview("ext-001", "Great service!", (short) 5, Sentiment.POSITIVE);

        // When
        Review savedReview = reviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Review> foundReview = reviewRepository.findById(savedReview.getId());
        assertThat(foundReview).isPresent();
        assertThat(foundReview.get().getExternalReviewId()).isEqualTo("ext-001");
        assertThat(foundReview.get().getContent()).isEqualTo("Great service!");
        assertThat(foundReview.get().getRating()).isEqualTo((short) 5);
        assertThat(foundReview.get().getSentiment()).isEqualTo(Sentiment.POSITIVE);
        assertThat(foundReview.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find review by ID and review source ID")
    void shouldFindReviewByIdAndReviewSourceId() {
        // Given
        Review review = createReview("ext-001", "Good!", (short) 4, Sentiment.POSITIVE);
        entityManager.persistAndFlush(review);
        entityManager.clear();

        // When
        Optional<Review> found = reviewRepository.findByIdAndReviewSourceId(
                review.getId(),
                reviewSource.getId()
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(review.getId());
    }

    @Test
    @DisplayName("Should find review by review source ID and external review ID")
    void shouldFindByReviewSourceIdAndExternalReviewId() {
        // Given
        Review review = createReview("ext-unique-123", "Test content", (short) 3, Sentiment.NEUTRAL);
        entityManager.persistAndFlush(review);
        entityManager.clear();

        // When
        Optional<Review> found = reviewRepository.findByReviewSourceIdAndExternalReviewId(
                reviewSource.getId(),
                "ext-unique-123"
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getExternalReviewId()).isEqualTo("ext-unique-123");
    }

    @Test
    @DisplayName("Should find all reviews for a review source with pagination")
    void shouldFindByReviewSourceIdWithPagination() {
        // Given
        for (int i = 1; i <= 15; i++) {
            Review review = createReview("ext-" + i, "Review " + i, (short) 4, Sentiment.POSITIVE);
            entityManager.persist(review);
        }
        entityManager.flush();
        entityManager.clear();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> page = reviewRepository.findByReviewSourceId(reviewSource.getId(), pageable);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(10);
    }

    @Test
    @DisplayName("Should find all reviews for a brand (across all sources)")
    void shouldFindByBrandId() {
        // Given
        ReviewSource source2 = ReviewSource.builder()
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page")
                .externalProfileId("fb-456")
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source2);

        // Create reviews for source 1
        for (int i = 1; i <= 5; i++) {
            Review review = createReview("google-" + i, "Google review " + i, (short) 5, Sentiment.POSITIVE);
            review.setReviewSource(reviewSource);
            entityManager.persist(review);
        }

        // Create reviews for source 2
        for (int i = 1; i <= 3; i++) {
            Review review = createReview("fb-" + i, "Facebook review " + i, (short) 4, Sentiment.POSITIVE);
            review.setReviewSource(source2);
            entityManager.persist(review);
        }

        entityManager.flush();
        entityManager.clear();

        // When
        Pageable pageable = PageRequest.of(0, 20);
        Page<Review> page = reviewRepository.findByBrandId(brand.getId(), pageable);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(8); // 5 + 3
    }

    @Test
    @DisplayName("Should find reviews by sentiment filter")
    void shouldFindByReviewSourceIdAndSentiment() {
        // Given
        createAndPersistReview("ext-1", "Excellent!", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-2", "Very good", (short) 4, Sentiment.POSITIVE);
        createAndPersistReview("ext-3", "Terrible", (short) 1, Sentiment.NEGATIVE);
        createAndPersistReview("ext-4", "Bad service", (short) 2, Sentiment.NEGATIVE);
        createAndPersistReview("ext-5", "Okay", (short) 3, Sentiment.NEUTRAL);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> positiveReviews = reviewRepository.findByReviewSourceIdAndSentiment(
                reviewSource.getId(), Sentiment.POSITIVE, pageable);
        Page<Review> negativeReviews = reviewRepository.findByReviewSourceIdAndSentiment(
                reviewSource.getId(), Sentiment.NEGATIVE, pageable);

        // Then
        assertThat(positiveReviews.getTotalElements()).isEqualTo(2);
        assertThat(negativeReviews.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find reviews by rating filter")
    void shouldFindByReviewSourceIdAndRatingIn() {
        // Given
        createAndPersistReview("ext-1", "Review 1", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-2", "Review 2", (short) 4, Sentiment.POSITIVE);
        createAndPersistReview("ext-3", "Review 3", (short) 3, Sentiment.NEUTRAL);
        createAndPersistReview("ext-4", "Review 4", (short) 2, Sentiment.NEGATIVE);
        createAndPersistReview("ext-5", "Review 5", (short) 1, Sentiment.NEGATIVE);
        entityManager.flush();

        // When - filter for low ratings (1 and 2 stars)
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> lowRatings = reviewRepository.findByReviewSourceIdAndRatingIn(
                reviewSource.getId(),
                List.of((short) 1, (short) 2),
                pageable
        );

        // Then
        assertThat(lowRatings.getTotalElements()).isEqualTo(2);
        assertThat(lowRatings.getContent())
                .extracting(Review::getRating)
                .containsExactlyInAnyOrder((short) 1, (short) 2);
    }

    @Test
    @DisplayName("Should find negative reviews (rating <= 2)")
    void shouldFindNegativeReviewsByReviewSourceId() {
        // Given
        createAndPersistReview("ext-1", "Great!", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-2", "Bad", (short) 2, Sentiment.NEGATIVE);
        createAndPersistReview("ext-3", "Terrible", (short) 1, Sentiment.NEGATIVE);
        createAndPersistReview("ext-4", "Okay", (short) 3, Sentiment.NEUTRAL);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> negativeReviews = reviewRepository.findNegativeReviewsByReviewSourceId(
                reviewSource.getId(), pageable);

        // Then
        assertThat(negativeReviews.getTotalElements()).isEqualTo(2);
        assertThat(negativeReviews.getContent())
                .allMatch(review -> review.getRating() <= 2);
    }

    @Test
    @DisplayName("Should find reviews within date range")
    void shouldFindByReviewSourceIdAndPublishedAtBetween() {
        // Given
        Instant now = Instant.now();
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);
        Instant tenDaysAgo = now.minus(10, ChronoUnit.DAYS);

        Review recentReview = createReview("ext-1", "Recent", (short) 5, Sentiment.POSITIVE);
        recentReview.setPublishedAt(twoDaysAgo);
        entityManager.persist(recentReview);

        Review oldReview = createReview("ext-2", "Old", (short) 4, Sentiment.POSITIVE);
        oldReview.setPublishedAt(tenDaysAgo);
        entityManager.persist(oldReview);

        entityManager.flush();

        // When - query for reviews in last 7 days
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> recentReviews = reviewRepository.findByReviewSourceIdAndPublishedAtBetween(
                reviewSource.getId(),
                sevenDaysAgo,
                now,
                pageable
        );

        // Then
        assertThat(recentReviews.getTotalElements()).isEqualTo(1);
        assertThat(recentReviews.getContent().get(0).getExternalReviewId()).isEqualTo("ext-1");
    }

    @Test
    @DisplayName("Should find reviews with multiple filters (composite query)")
    void shouldFindByMultipleFilters() {
        // Given
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        Instant tenDaysAgo = now.minus(10, ChronoUnit.DAYS);

        // Recent negative reviews (should match)
        Review match1 = createReview("ext-1", "Bad recent", (short) 1, Sentiment.NEGATIVE);
        match1.setPublishedAt(yesterday);
        entityManager.persist(match1);

        Review match2 = createReview("ext-2", "Poor recent", (short) 2, Sentiment.NEGATIVE);
        match2.setPublishedAt(threeDaysAgo);
        entityManager.persist(match2);

        // Old negative review (should NOT match - outside date range)
        Review noMatch1 = createReview("ext-3", "Bad old", (short) 1, Sentiment.NEGATIVE);
        noMatch1.setPublishedAt(tenDaysAgo);
        entityManager.persist(noMatch1);

        // Recent positive review (should NOT match - wrong sentiment)
        Review noMatch2 = createReview("ext-4", "Good recent", (short) 5, Sentiment.POSITIVE);
        noMatch2.setPublishedAt(yesterday);
        entityManager.persist(noMatch2);

        entityManager.flush();

        // When - filter for negative reviews in last 7 days with ratings 1-2
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("publishedAt").descending());

        Page<Review> filteredReviews = reviewRepository.findByMultipleFilters(
                reviewSource.getId(),
                List.of(Sentiment.NEGATIVE),
                List.of((short) 1, (short) 2),
                sevenDaysAgo,
                now,
                pageable
        );

        // Then
        assertThat(filteredReviews.getTotalElements()).isEqualTo(2);
        assertThat(filteredReviews.getContent())
                .extracting(Review::getExternalReviewId)
                .containsExactly("ext-1", "ext-2"); // Ordered by publishedAt desc
    }

    @Test
    @DisplayName("Should find reviews for brand with multiple filters (aggregated view)")
    void shouldFindByBrandIdAndMultipleFilters() {
        // Given - Create second review source
        ReviewSource source2 = ReviewSource.builder()
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page")
                .externalProfileId("fb-456")
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .brand(brand)
                .build();
        entityManager.persist(source2);

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        // Review from source 1
        Review review1 = createReview("google-1", "Bad Google", (short) 1, Sentiment.NEGATIVE);
        review1.setPublishedAt(yesterday);
        review1.setReviewSource(reviewSource);
        entityManager.persist(review1);

        // Review from source 2
        Review review2 = createReview("fb-1", "Bad Facebook", (short) 2, Sentiment.NEGATIVE);
        review2.setPublishedAt(yesterday);
        review2.setReviewSource(source2);
        entityManager.persist(review2);

        entityManager.flush();

        // When - search across all brand sources
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Review> allBrandNegativeReviews = reviewRepository.findByBrandIdAndMultipleFilters(
                brand.getId(),
                List.of(Sentiment.NEGATIVE),
                List.of((short) 1, (short) 2),
                twoDaysAgo,
                now,
                pageable
        );

        // Then
        assertThat(allBrandNegativeReviews.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count reviews by sentiment")
    void shouldCountByReviewSourceIdAndSentiment() {
        // Given
        createAndPersistReview("ext-1", "Positive 1", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-2", "Positive 2", (short) 4, Sentiment.POSITIVE);
        createAndPersistReview("ext-3", "Negative", (short) 1, Sentiment.NEGATIVE);
        createAndPersistReview("ext-4", "Neutral", (short) 3, Sentiment.NEUTRAL);
        entityManager.flush();

        // When
        long positiveCount = reviewRepository.countByReviewSourceIdAndSentiment(
                reviewSource.getId(), Sentiment.POSITIVE);
        long negativeCount = reviewRepository.countByReviewSourceIdAndSentiment(
                reviewSource.getId(), Sentiment.NEGATIVE);
        long neutralCount = reviewRepository.countByReviewSourceIdAndSentiment(
                reviewSource.getId(), Sentiment.NEUTRAL);

        // Then
        assertThat(positiveCount).isEqualTo(2);
        assertThat(negativeCount).isEqualTo(1);
        assertThat(neutralCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count reviews by rating")
    void shouldCountByReviewSourceIdAndRating() {
        // Given
        createAndPersistReview("ext-1", "5 stars", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-2", "5 stars", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-3", "1 star", (short) 1, Sentiment.NEGATIVE);
        entityManager.flush();

        // When
        long fiveStarCount = reviewRepository.countByReviewSourceIdAndRating(
                reviewSource.getId(), (short) 5);
        long oneStarCount = reviewRepository.countByReviewSourceIdAndRating(
                reviewSource.getId(), (short) 1);

        // Then
        assertThat(fiveStarCount).isEqualTo(2);
        assertThat(oneStarCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate average rating")
    void shouldCalculateAverageRating() {
        // Given
        createAndPersistReview("ext-1", "Review 1", (short) 5, Sentiment.POSITIVE);
        createAndPersistReview("ext-2", "Review 2", (short) 4, Sentiment.POSITIVE);
        createAndPersistReview("ext-3", "Review 3", (short) 3, Sentiment.NEUTRAL);
        entityManager.flush();

        // When
        Double avgRating = reviewRepository.calculateAverageRating(reviewSource.getId());

        // Then
        assertThat(avgRating).isEqualTo(4.0); // (5 + 4 + 3) / 3
    }

    @Test
    @DisplayName("Should find top N recent negative reviews")
    void shouldFindTopNRecentNegativeReviews() {
        // Given
        Instant now = Instant.now();
        for (int i = 1; i <= 10; i++) {
            Review review = createReview("ext-" + i, "Negative " + i, (short) 1, Sentiment.NEGATIVE);
            review.setPublishedAt(now.minus(i, ChronoUnit.DAYS));
            entityManager.persist(review);
        }
        entityManager.flush();

        // When - get top 3 most recent
        Pageable pageable = PageRequest.of(0, 3);
        List<Review> topRecent = reviewRepository.findTopNRecentNegativeReviews(
                reviewSource.getId(), pageable);

        // Then
        assertThat(topRecent).hasSize(3);
        assertThat(topRecent.get(0).getExternalReviewId()).isEqualTo("ext-1"); // Most recent
        assertThat(topRecent.get(1).getExternalReviewId()).isEqualTo("ext-2");
        assertThat(topRecent.get(2).getExternalReviewId()).isEqualTo("ext-3");
    }

    @Test
    @DisplayName("Should not find soft-deleted reviews")
    void shouldNotFindSoftDeletedReviews() {
        // Given
        Review activeReview = createReview("ext-active", "Active", (short) 5, Sentiment.POSITIVE);
        Review deletedReview = createReview("ext-deleted", "Deleted", (short) 4, Sentiment.POSITIVE);
        deletedReview.softDelete();

        entityManager.persist(activeReview);
        entityManager.persist(deletedReview);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> allReviews = reviewRepository.findByReviewSourceId(reviewSource.getId(), pageable);

        // Then - only active review should be found
        assertThat(allReviews.getTotalElements()).isEqualTo(1);
        assertThat(allReviews.getContent().get(0).getExternalReviewId()).isEqualTo("ext-active");
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

    private void createAndPersistReview(String externalId, String content, Short rating, Sentiment sentiment) {
        Review review = createReview(externalId, content, rating, sentiment);
        entityManager.persist(review);
    }
}
