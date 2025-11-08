package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.dashboard.*;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.repository.ReviewRepository;
import com.morawski.dev.backend.repository.ReviewSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock
    private BrandService brandService;

    @Mock
    private ReviewSourceService reviewSourceService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private SyncJobService syncJobService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewSourceRepository reviewSourceRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private Brand testBrand;
    private ReviewSource testReviewSource;

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
            .profileUrl("https://g.page/test")
            .isActive(true)
            .brand(testBrand)
            .lastSyncAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("getDashboard() Tests")
    class GetDashboardTests {

        @Test
        @DisplayName("Should return dashboard with aggregated metrics for all sources")
        void shouldReturnDashboard_WithAggregatedMetrics() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(List.of(testReviewSource));
            when(reviewRepository.countByReviewSourceId(1L)).thenReturn(100L);
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.5);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.POSITIVE)).thenReturn(75L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEGATIVE)).thenReturn(15L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEUTRAL)).thenReturn(10L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(1L), anyShort())).thenReturn(20L);
            when(reviewService.getRecentNegativeReviews(1L, 5)).thenReturn(new ArrayList<>());

            // When
            DashboardResponse response = dashboardService.getDashboard(1L, null, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBrandId()).isEqualTo(1L);
            assertThat(response.getBrandName()).isEqualTo("Test Brand");
            assertThat(response.getSources()).hasSize(1);
            assertThat(response.getMetrics()).isNotNull();
            assertThat(response.getMetrics().getTotalReviews()).isEqualTo(100L);
            assertThat(response.getMetrics().getAverageRating()).isEqualTo(4.5);

            verify(reviewRepository, atLeastOnce()).countByReviewSourceId(1L);
        }

        @Test
        @DisplayName("Should return dashboard for specific source when sourceId provided")
        void shouldReturnDashboard_ForSpecificSource() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(List.of(testReviewSource));
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(reviewRepository.countByReviewSourceId(1L)).thenReturn(50L);
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.2);
            when(reviewRepository.countByReviewSourceIdAndSentiment(eq(1L), any(Sentiment.class))).thenReturn(10L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(1L), anyShort())).thenReturn(10L);
            when(reviewService.getRecentNegativeReviews(1L, 5)).thenReturn(new ArrayList<>());

            // When
            DashboardResponse response = dashboardService.getDashboard(1L, 1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSelectedSourceId()).isEqualTo(1L);
            assertThat(response.getMetrics().getTotalReviews()).isEqualTo(50L);

            verify(reviewSourceService).findByIdAndBrandIdOrThrow(1L, 1L);
        }

        @Test
        @DisplayName("Should return empty dashboard when no sources configured")
        void shouldReturnEmptyDashboard_WhenNoSources() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(new ArrayList<>());

            // When
            DashboardResponse response = dashboardService.getDashboard(1L, null, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBrandId()).isEqualTo(1L);
            assertThat(response.getSources()).isEmpty();
            assertThat(response.getMetrics().getTotalReviews()).isEqualTo(0);
            assertThat(response.getSummaryText()).contains("No review sources configured yet");

            verify(reviewRepository, never()).countByReviewSourceId(anyLong());
        }

        @Test
        @DisplayName("Should calculate sentiment percentages correctly")
        void shouldCalculateSentimentPercentages_Correctly() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(List.of(testReviewSource));
            when(reviewRepository.countByReviewSourceId(1L)).thenReturn(100L);
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.0);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.POSITIVE)).thenReturn(70L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEGATIVE)).thenReturn(20L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEUTRAL)).thenReturn(10L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(1L), anyShort())).thenReturn(20L);
            when(reviewService.getRecentNegativeReviews(1L, 5)).thenReturn(new ArrayList<>());

            // When
            DashboardResponse response = dashboardService.getDashboard(1L, null, 1L);

            // Then
            assertThat(response.getMetrics().getPositivePercentage()).isEqualTo(70.0);
            assertThat(response.getMetrics().getNegativePercentage()).isEqualTo(20.0);
            assertThat(response.getMetrics().getNeutralPercentage()).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("getSourceSummary() Tests")
    class GetSourceSummaryTests {

        @Test
        @DisplayName("Should return source summary with metrics")
        void shouldReturnSourceSummary_WithMetrics() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(reviewRepository.countByReviewSourceId(1L)).thenReturn(50L);
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.3);
            when(reviewRepository.countByReviewSourceIdAndSentiment(eq(1L), any(Sentiment.class))).thenReturn(10L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(1L), anyShort())).thenReturn(10L);
            when(reviewService.getRecentNegativeReviews(1L, 5)).thenReturn(new ArrayList<>());
            when(syncJobService.getInitialImportJob(1L)).thenReturn(null);

            // When
            SourceSummaryResponse response = dashboardService.getSourceSummary(1L, 1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSourceId()).isEqualTo(1L);
            assertThat(response.getSourceType()).isEqualTo(SourceType.GOOGLE);
            assertThat(response.getMetrics()).isNotNull();
            assertThat(response.getMetrics().getTotalReviews()).isEqualTo(50L);
            assertThat(response.getMetrics().getAverageRating()).isEqualTo(4.3);

            verify(reviewSourceService).findByIdAndBrandIdOrThrow(1L, 1L);
        }
    }

    @Nested
    @DisplayName("Metrics Calculation Tests")
    class MetricsCalculationTests {

        @Test
        @DisplayName("Should calculate metrics for multiple sources")
        void shouldCalculateMetrics_ForMultipleSources() {
            // Given
            ReviewSource source2 = ReviewSource.builder()
                .id(2L)
                .sourceType(SourceType.FACEBOOK)
                .build();

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(List.of(testReviewSource, source2));

            // Source 1 metrics
            when(reviewRepository.countByReviewSourceId(1L)).thenReturn(100L);
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.5);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.POSITIVE)).thenReturn(70L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEGATIVE)).thenReturn(20L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEUTRAL)).thenReturn(10L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(1L), anyShort())).thenReturn(20L);

            // Source 2 metrics
            when(reviewRepository.countByReviewSourceId(2L)).thenReturn(50L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.0);
            when(reviewRepository.countByReviewSourceIdAndSentiment(2L, Sentiment.POSITIVE)).thenReturn(30L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(2L, Sentiment.NEGATIVE)).thenReturn(15L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(2L, Sentiment.NEUTRAL)).thenReturn(5L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(2L), anyShort())).thenReturn(10L);

            when(reviewService.getRecentNegativeReviews(anyLong(), anyInt())).thenReturn(new ArrayList<>());

            // When
            DashboardResponse response = dashboardService.getDashboard(1L, null, 1L);

            // Then
            assertThat(response.getMetrics().getTotalReviews()).isEqualTo(150L); // 100 + 50
            assertThat(response.getSources()).hasSize(2);

            verify(reviewRepository, atLeastOnce()).countByReviewSourceId(1L);
            verify(reviewRepository, atLeastOnce()).countByReviewSourceId(2L);
        }

        @Test
        @DisplayName("Should generate appropriate summary text based on sentiment")
        void shouldGenerateSummaryText_BasedOnSentiment() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(List.of(testReviewSource));
            when(reviewRepository.countByReviewSourceId(1L)).thenReturn(100L);
            when(reviewRepository.calculateAverageRating(1L)).thenReturn(4.8);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.POSITIVE)).thenReturn(85L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEGATIVE)).thenReturn(10L);
            when(reviewRepository.countByReviewSourceIdAndSentiment(1L, Sentiment.NEUTRAL)).thenReturn(5L);
            when(reviewRepository.countByReviewSourceIdAndRating(eq(1L), anyShort())).thenReturn(20L);
            when(reviewService.getRecentNegativeReviews(1L, 5)).thenReturn(new ArrayList<>());

            // When
            DashboardResponse response = dashboardService.getDashboard(1L, null, 1L);

            // Then
            assertThat(response.getSummaryText()).contains("85% positive reviews");
            assertThat(response.getSummaryText()).containsPattern("4[.,]8/5\\.0");
        }
    }
}
