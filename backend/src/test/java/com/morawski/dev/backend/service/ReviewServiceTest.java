package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.review.*;
import com.morawski.dev.backend.entity.*;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.mapper.ReviewMapper;
import com.morawski.dev.backend.mapper.SentimentChangeMapper;
import com.morawski.dev.backend.repository.ReviewRepository;
import com.morawski.dev.backend.repository.SentimentChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SentimentChangeRepository sentimentChangeRepository;

    @Mock
    private BrandService brandService;

    @Mock
    private ReviewSourceService reviewSourceService;

    @Mock
    private UserService userService;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private SentimentChangeMapper sentimentChangeMapper;

    @InjectMocks
    private ReviewService reviewService;

    private Brand testBrand;
    private ReviewSource testReviewSource;
    private Review testReview;
    private ReviewResponse testReviewResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .build();

        testBrand = Brand.builder()
            .id(1L)
            .name("Test Brand")
            .user(testUser)
            .build();

        testReviewSource = ReviewSource.builder()
            .id(1L)
            .sourceType(SourceType.GOOGLE)
            .brand(testBrand)
            .build();

        testReview = Review.builder()
            .id(1L)
            .externalReviewId("google-review-123")
            .content("Great service!")
            .authorName("John Doe")
            .rating((short) 5)
            .sentiment(Sentiment.POSITIVE)
            .sentimentConfidence(java.math.BigDecimal.valueOf(0.95))
            .publishedAt(Instant.now())
            .fetchedAt(Instant.now())
            .reviewSource(testReviewSource)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        testReviewResponse = ReviewResponse.builder()
            .reviewId(1L)
            .sourceId(1L)
            .sourceType(SourceType.GOOGLE)
            .content("Great service!")
            .authorName("John Doe")
            .rating(5)
            .sentiment(Sentiment.POSITIVE)
            .publishedAt(Instant.now().atZone(ZoneId.of("UTC")))
            .build();
    }

    @Nested
    @DisplayName("getReviews() Tests")
    class GetReviewsTests {

        @Test
        @DisplayName("Should return paginated reviews for brand")
        void shouldReturnPaginatedReviews_ForBrand() {
            // Given
            Page<Review> reviewPage = new PageImpl<>(List.of(testReview));

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewRepository.findByBrandIdAndMultipleFilters(eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(reviewPage);
            when(reviewMapper.toReviewResponse(testReview)).thenReturn(testReviewResponse);

            // When
            ReviewListResponse response = reviewService.getReviews(
                1L, null, null, null, null, null, 0, 20, "publishedAt", "desc", 1L
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getReviews()).hasSize(1);
            assertThat(response.getPagination().getTotalItems()).isEqualTo(1L);

            verify(reviewRepository).findByBrandIdAndMultipleFilters(eq(1L), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter reviews by source ID")
        void shouldFilterReviews_BySourceId() {
            // Given
            Page<Review> reviewPage = new PageImpl<>(List.of(testReview));

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(reviewRepository.findByMultipleFilters(eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(reviewPage);
            when(reviewMapper.toReviewResponse(testReview)).thenReturn(testReviewResponse);

            // When
            ReviewListResponse response = reviewService.getReviews(
                1L, 1L, null, null, null, null, 0, 20, "publishedAt", "desc", 1L
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getReviews()).hasSize(1);
            assertThat(response.getFilters().getSourceId()).isEqualTo(1L);

            verify(reviewRepository).findByMultipleFilters(eq(1L), any(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getReviewById() Tests")
    class GetReviewByIdTests {

        @Test
        @DisplayName("Should return review by ID")
        void shouldReturnReview_ById() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
            when(sentimentChangeRepository.findByReviewIdOrderByChangedAtDesc(1L)).thenReturn(List.of());

            // When
            ReviewDetailResponse response = reviewService.getReviewById(1L, 1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getReviewId()).isEqualTo(1L);
            assertThat(response.getContent()).isEqualTo("Great service!");

            verify(reviewRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review not found")
        void shouldThrowException_WhenReviewNotFound() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reviewService.getReviewById(1L, 999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(reviewRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw ResourceAccessDeniedException when review doesn't belong to brand")
        void shouldThrowException_WhenReviewDoesNotBelongToBrand() {
            // Given
            Brand anotherBrand = Brand.builder().id(2L).build();
            ReviewSource anotherSource = ReviewSource.builder().id(2L).brand(anotherBrand).build();
            Review anotherReview = Review.builder()
                .id(2L)
                .reviewSource(anotherSource)
                .build();

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewRepository.findById(2L)).thenReturn(Optional.of(anotherReview));

            // When & Then
            assertThatThrownBy(() -> reviewService.getReviewById(1L, 2L, 1L))
                .isInstanceOf(ResourceAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("updateReviewSentiment() Tests")
    class UpdateReviewSentimentTests {

        @Test
        @DisplayName("Should update review sentiment successfully")
        void shouldUpdateReviewSentiment_Successfully() {
            // Given
            UpdateReviewSentimentRequest request = new UpdateReviewSentimentRequest();
            request.setSentiment(Sentiment.NEGATIVE);

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(sentimentChangeRepository.save(any(SentimentChange.class)))
                .thenAnswer(invocation -> {
                    SentimentChange change = invocation.getArgument(0);
                    change.setId(1L);
                    return change;
                });
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

            // When
            UpdateReviewSentimentResponse response = reviewService.updateReviewSentiment(
                1L, 1L, 1L, request
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getReviewId()).isEqualTo(1L);
            assertThat(response.getPreviousSentiment()).isEqualTo(Sentiment.POSITIVE);

            ArgumentCaptor<SentimentChange> changeCaptor = ArgumentCaptor.forClass(SentimentChange.class);
            verify(sentimentChangeRepository).save(changeCaptor.capture());
            SentimentChange savedChange = changeCaptor.getValue();
            assertThat(savedChange.getOldSentiment()).isEqualTo(Sentiment.POSITIVE);
            assertThat(savedChange.getNewSentiment()).isEqualTo(Sentiment.NEGATIVE);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(reviewCaptor.capture());
            Review savedReview = reviewCaptor.getValue();
            assertThat(savedReview.getSentiment()).isEqualTo(Sentiment.NEGATIVE);
        }

        @Test
        @DisplayName("Should not create change record when sentiment is the same")
        void shouldNotCreateChange_WhenSentimentSame() {
            // Given
            UpdateReviewSentimentRequest request = new UpdateReviewSentimentRequest();
            request.setSentiment(Sentiment.POSITIVE); // Same as current

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

            // When
            UpdateReviewSentimentResponse response = reviewService.updateReviewSentiment(
                1L, 1L, 1L, request
            );

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSentimentChangeId()).isNull();

            verify(sentimentChangeRepository, never()).save(any());
            verify(reviewRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("Should calculate average rating")
        void shouldCalculateAverageRating() {
            // Given
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.5);

            // When
            Double avgRating = reviewService.calculateAverageRating(1L);

            // Then
            assertThat(avgRating).isEqualTo(4.5);
        }

        @Test
        @DisplayName("Should count reviews by sentiment")
        void shouldCountReviews_BySentiment() {
            // Given
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.POSITIVE)).thenReturn(10L);

            // When
            long count = reviewService.countBySentiment(1L, Sentiment.POSITIVE);

            // Then
            assertThat(count).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should count reviews by rating")
        void shouldCountReviews_ByRating() {
            // Given
            when(reviewRepository.countByReviewSourceIdAndRating(1L, (short) 5)).thenReturn(15L);

            // When
            long count = reviewService.countByRating(1L, (short) 5);

            // Then
            assertThat(count).isEqualTo(15L);
        }
    }
}
