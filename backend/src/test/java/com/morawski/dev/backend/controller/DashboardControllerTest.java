package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.dashboard.AISummaryResponse;
import com.morawski.dev.backend.dto.dashboard.DashboardSummaryResponse;
import com.morawski.dev.backend.dto.dashboard.MetricsResponse;
import com.morawski.dev.backend.dto.dashboard.SentimentDistributionResponse;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.service.AISummaryService;
import com.morawski.dev.backend.service.BrandService;
import com.morawski.dev.backend.service.DashboardService;
import com.morawski.dev.backend.service.ReviewSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for DashboardController using @WebMvcTest.
 * Tests dashboard and AI summary endpoints with MockMvc.
 */
@WebMvcTest(DashboardController.class)
@DisplayName("DashboardController Integration Tests")
class DashboardControllerTest extends ControllerTestBase {

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private AISummaryService aiSummaryService;

    @MockBean
    private ReviewSourceService reviewSourceService;

    @MockBean
    private BrandService brandService;

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Nested
    @DisplayName("GET /api/dashboard/summary")
    class GetDashboardSummaryTests {

        private DashboardSummaryResponse sampleSummary(Long brandId, Long sourceId) {
            SentimentDistributionResponse sentiment = SentimentDistributionResponse.builder()
                .positive(70).negative(20).neutral(10)
                .positivePercentage(BigDecimal.valueOf(70.0))
                .negativePercentage(BigDecimal.valueOf(20.0))
                .neutralPercentage(BigDecimal.valueOf(10.0))
                .build();

            MetricsResponse metrics = MetricsResponse.builder()
                .totalReviews(100)
                .averageRating(BigDecimal.valueOf(4.5))
                .sentimentDistribution(sentiment)
                .ratingDistribution(Map.of(1, 10, 2, 10, 3, 10, 4, 20, 5, 50))
                .build();

            AISummaryResponse ai = AISummaryResponse.builder()
                .summaryId(1L)
                .sourceId(sourceId)
                .text("Overall positive feedback with high ratings")
                .build();

            return DashboardSummaryResponse.builder()
                .brandId(brandId)
                .sourceId(sourceId)
                .metrics(metrics)
                .aiSummary(ai)
                .recentNegativeReviews(List.of())
                .build();
        }

        @Test
        @DisplayName("Should get dashboard summary successfully and return 200 OK")
        void shouldGetDashboardSummary_Successfully() throws Exception {
            // Given
            when(dashboardService.getDashboardSummary(1L, null, 1L)).thenReturn(sampleSummary(1L, null));

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary")
                .param("brandId", "1")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.metrics.averageRating").value(4.5))
                .andExpect(jsonPath("$.metrics.totalReviews").value(100))
                .andExpect(jsonPath("$.metrics.sentimentDistribution.positivePercentage").value(70.0))
                .andExpect(jsonPath("$.aiSummary.text").value("Overall positive feedback with high ratings"));

            verify(dashboardService).getDashboardSummary(1L, null, 1L);
        }

        @Test
        @DisplayName("Should get dashboard summary filtered by source successfully")
        void shouldGetDashboardSummaryBySource_Successfully() throws Exception {
            // Given
            when(dashboardService.getDashboardSummary(1L, 1L, 1L)).thenReturn(sampleSummary(1L, 1L));

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary")
                .param("brandId", "1")
                .param("sourceId", "1")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.sourceId").value(1))
                .andExpect(jsonPath("$.metrics.totalReviews").value(100));

            verify(dashboardService).getDashboardSummary(1L, 1L, 1L);
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own brand")
        void shouldReturnForbidden_WhenUserDoesNotOwnBrand() throws Exception {
            // Given
            when(dashboardService.getDashboardSummary(2L, null, 1L))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary")
                .param("brandId", "2")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(dashboardService).getDashboardSummary(2L, null, 1L);
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when brand doesn't exist")
        void shouldReturnNotFound_WhenBrandDoesNotExist() throws Exception {
            // Given
            when(dashboardService.getDashboardSummary(999L, null, 1L))
                .thenThrow(new ResourceNotFoundException("Brand", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary")
                .param("brandId", "999")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(dashboardService).getDashboardSummary(999L, null, 1L);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary")
                .param("brandId", "1")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(dashboardService, never()).getDashboardSummary(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/dashboard/summary/ai")
    class GetAISummaryTests {

        @Test
        @DisplayName("Should get AI summary successfully and return 200 OK")
        void shouldGetAISummary_Successfully() throws Exception {
            // Given
            Brand brand = new Brand();
            brand.setId(1L);

            ReviewSource source = new ReviewSource();
            source.setId(1L);
            source.setBrand(brand);

            AISummaryResponse response = AISummaryResponse.builder()
                .summaryId(1L)
                .sourceId(1L)
                .text("Customers love the fast service and friendly staff. Some concerns about pricing.")
                .generatedAt(ZonedDateTime.now())
                .validUntil(ZonedDateTime.now().plusDays(1))
                .build();

            when(reviewSourceService.findByIdOrThrow(1L)).thenReturn(source);
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(brand);
            when(aiSummaryService.getSummaryForSource(1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary/ai")
                .param("sourceId", "1")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryId").value(1))
                .andExpect(jsonPath("$.sourceId").value(1))
                .andExpect(jsonPath("$.text").exists());

            verify(reviewSourceService).findByIdOrThrow(1L);
            verify(brandService).findByIdAndUserIdOrThrow(1L, 1L);
            verify(aiSummaryService).getSummaryForSource(1L);
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when source doesn't exist")
        void shouldReturnNotFound_WhenSourceDoesNotExist() throws Exception {
            // Given
            when(reviewSourceService.findByIdOrThrow(999L))
                .thenThrow(new ResourceNotFoundException("ReviewSource", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary/ai")
                .param("sourceId", "999")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(reviewSourceService).findByIdOrThrow(999L);
            verify(aiSummaryService, never()).getSummaryForSource(any());
        }

        @Test
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own source")
        void shouldReturnForbidden_WhenUserDoesNotOwnSource() throws Exception {
            // Given
            Brand brand = new Brand();
            brand.setId(2L);

            ReviewSource source = new ReviewSource();
            source.setId(1L);
            source.setBrand(brand);

            when(reviewSourceService.findByIdOrThrow(1L)).thenReturn(source);
            when(brandService.findByIdAndUserIdOrThrow(2L, 1L))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(get("/api/dashboard/summary/ai")
                .param("sourceId", "1")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(reviewSourceService).findByIdOrThrow(1L);
            verify(brandService).findByIdAndUserIdOrThrow(2L, 1L);
            verify(aiSummaryService, never()).getSummaryForSource(any());
        }
    }
}
