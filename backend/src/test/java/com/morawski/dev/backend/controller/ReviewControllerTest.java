package com.morawski.dev.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.review.*;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.security.test.WithMockCustomUser;
import com.morawski.dev.backend.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;

import com.morawski.dev.backend.dto.common.PaginationResponse;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ReviewController using @WebMvcTest.
 * Tests review management endpoints with MockMvc.
 */
@WebMvcTest(ReviewController.class)
@DisplayName("ReviewController Integration Tests")
class ReviewControllerTest extends ControllerTestBase {
    @MockBean
    private ReviewService reviewService;

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Nested
    @DisplayName("GET /api/brands/{brandId}/reviews")
    class GetReviewsTests {

        @Test
        @WithMockCustomUser
        @DisplayName("Should get reviews with pagination successfully and return 200 OK")
        void shouldGetReviews_Successfully() throws Exception {
            // Given
            ReviewResponse review1 = ReviewResponse.builder()
                .reviewId(1L)
                .sourceId(1L)
                .content("Great service!")
                .rating(5)
                .sentiment(Sentiment.POSITIVE)
                .authorName("John Doe")
                .publishedAt(ZonedDateTime.now())
                .fetchedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

            ReviewResponse review2 = ReviewResponse.builder()
                .reviewId(2L)
                .sourceId(1L)
                .content("Not satisfied")
                .rating(2)
                .sentiment(Sentiment.NEGATIVE)
                .authorName("Jane Smith")
                .publishedAt(ZonedDateTime.now())
                .fetchedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(2L)
                .totalPages(1)
                .build();

            ReviewListResponse response = ReviewListResponse.builder()
                .reviews(List.of(review1, review2))
                .pagination(pagination)
                .build();

            when(reviewService.getReviews(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            )).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/reviews")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews[0].reviewId").value(1))
                .andExpect(jsonPath("$.reviews[0].sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$.reviews[1].reviewId").value(2))
                .andExpect(jsonPath("$.reviews[1].sentiment").value("NEGATIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

            verify(reviewService).getReviews(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            );
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should get reviews filtered by sentiment successfully")
        void shouldGetReviewsFilteredBySentiment_Successfully() throws Exception {
            // Given
            ReviewResponse review = ReviewResponse.builder()
                .reviewId(1L)
                .sourceId(1L)
                .content("Not satisfied")
                .rating(2)
                .sentiment(Sentiment.NEGATIVE)
                .authorName("Jane Smith")
                .publishedAt(ZonedDateTime.now())
                .fetchedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(1L)
                .totalPages(1)
                .build();

            ReviewListResponse response = ReviewListResponse.builder()
                .reviews(List.of(review))
                .pagination(pagination)
                .build();

            when(reviewService.getReviews(
                eq(1L), isNull(), eq(List.of(Sentiment.NEGATIVE)), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            )).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/reviews")
                .param("sentiment", "NEGATIVE")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews[0].sentiment").value("NEGATIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));

            verify(reviewService).getReviews(
                eq(1L), isNull(), eq(List.of(Sentiment.NEGATIVE)), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            );
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should get reviews filtered by rating successfully")
        void shouldGetReviewsFilteredByRating_Successfully() throws Exception {
            // Given
            ReviewResponse review = ReviewResponse.builder()
                .reviewId(1L)
                .sourceId(1L)
                .content("Great service!")
                .rating(5)
                .sentiment(Sentiment.POSITIVE)
                .authorName("John Doe")
                .publishedAt(ZonedDateTime.now())
                .fetchedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(1L)
                .totalPages(1)
                .build();

            ReviewListResponse response = ReviewListResponse.builder()
                .reviews(List.of(review))
                .pagination(pagination)
                .build();

            when(reviewService.getReviews(
                eq(1L), isNull(), isNull(), eq(List.of((short) 5)), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            )).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/reviews")
                .param("rating", "5")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].rating").value(5));

            verify(reviewService).getReviews(
                eq(1L), isNull(), isNull(), eq(List.of((short) 5)), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            );
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should get reviews with custom pagination")
        void shouldGetReviewsWithCustomPagination_Successfully() throws Exception {
            // Given
            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(1)
                .pageSize(10)
                .totalItems(0L)
                .totalPages(0)
                .build();

            ReviewListResponse response = ReviewListResponse.builder()
                .reviews(List.of())
                .pagination(pagination)
                .build();

            when(reviewService.getReviews(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), eq(10), eq("publishedAt"), eq("desc"), eq(1L)
            )).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/reviews")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

            verify(reviewService).getReviews(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), eq(10), eq("publishedAt"), eq("desc"), eq(1L)
            );
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own brand")
        void shouldReturnForbidden_WhenUserDoesNotOwnBrand() throws Exception {
            // Given
            when(reviewService.getReviews(
                eq(2L), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("publishedAt"), eq("desc"), eq(1L)
            )).thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/2/reviews")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/brands/{brandId}/reviews/{reviewId}")
    class GetReviewByIdTests {

        @Test
        @WithMockCustomUser
        @DisplayName("Should get review by ID successfully and return 200 OK")
        void shouldGetReviewById_Successfully() throws Exception {
            // Given
            ReviewDetailResponse response = ReviewDetailResponse.builder()
                .reviewId(1L)
                .sourceId(1L)
                .content("Great service!")
                .rating(5)
                .sentiment(Sentiment.POSITIVE)
                .authorName("John Doe")
                .publishedAt(ZonedDateTime.now())
                .fetchedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .sentimentChangeHistory(List.of())
                .build();

            when(reviewService.getReviewById(1L, 1L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/reviews/1")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.content").value("Great service!"))
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"))
                .andExpect(jsonPath("$.rating").value(5));

            verify(reviewService).getReviewById(1L, 1L, 1L);
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should return 404 NOT FOUND when review doesn't exist")
        void shouldReturnNotFound_WhenReviewDoesNotExist() throws Exception {
            // Given
            when(reviewService.getReviewById(1L, 1L, 999L))
                .thenThrow(new ResourceNotFoundException("Review", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/reviews/999")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(reviewService).getReviewById(1L, 1L, 999L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/brands/{brandId}/reviews/{reviewId}/sentiment")
    class UpdateReviewSentimentTests {

        @Test
        @WithMockCustomUser
        @DisplayName("Should update review sentiment successfully and return 200 OK")
        void shouldUpdateReviewSentiment_Successfully() throws Exception {
            // Given
            UpdateReviewSentimentRequest request = new UpdateReviewSentimentRequest();
            request.setSentiment(Sentiment.POSITIVE);

            UpdateReviewSentimentResponse response = UpdateReviewSentimentResponse.builder()
                .reviewId(1L)
                .sentiment(Sentiment.POSITIVE)
                .updatedAt(ZonedDateTime.now())
                .build();

            when(reviewService.updateReviewSentiment(
                eq(1L), eq(1L), eq(1L), any(UpdateReviewSentimentRequest.class)
            )).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/1/reviews/1/sentiment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.sentiment").value("POSITIVE"));

            verify(reviewService).updateReviewSentiment(
                eq(1L), eq(1L), eq(1L), any(UpdateReviewSentimentRequest.class)
            );
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should return 400 BAD REQUEST when sentiment is invalid")
        void shouldReturnBadRequest_WhenSentimentInvalid() throws Exception {
            // Given
            UpdateReviewSentimentRequest request = new UpdateReviewSentimentRequest();
            // Missing sentiment

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/1/reviews/1/sentiment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(reviewService, never()).updateReviewSentiment(any(), any(), any(), any());
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should return 404 NOT FOUND when review doesn't exist")
        void shouldReturnNotFound_WhenReviewDoesNotExist() throws Exception {
            // Given
            UpdateReviewSentimentRequest request = new UpdateReviewSentimentRequest();
            request.setSentiment(Sentiment.POSITIVE);

            when(reviewService.updateReviewSentiment(
                eq(1L), eq(999L), eq(1L), any(UpdateReviewSentimentRequest.class)
            )).thenThrow(new ResourceNotFoundException("Review", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/1/reviews/999/sentiment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isNotFound());

            verify(reviewService).updateReviewSentiment(
                eq(1L), eq(999L), eq(1L), any(UpdateReviewSentimentRequest.class)
            );
        }

        @Test
        @WithMockCustomUser
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own review")
        void shouldReturnForbidden_WhenUserDoesNotOwnReview() throws Exception {
            // Given
            UpdateReviewSentimentRequest request = new UpdateReviewSentimentRequest();
            request.setSentiment(Sentiment.POSITIVE);

            when(reviewService.updateReviewSentiment(
                eq(2L), eq(1L), eq(1L), any(UpdateReviewSentimentRequest.class)
            )).thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/2/reviews/1/sentiment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isForbidden());

            verify(reviewService).updateReviewSentiment(
                eq(2L), eq(1L), eq(1L), any(UpdateReviewSentimentRequest.class)
            );
        }
    }

}

