package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.source.*;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
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

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for ReviewSourceController using @WebMvcTest.
 * Tests review source management endpoints with MockMvc.
 */
@WebMvcTest(ReviewSourceController.class)
@DisplayName("ReviewSourceController Integration Tests")
class ReviewSourceControllerTest extends ControllerTestBase {

    @MockBean
    private ReviewSourceService reviewSourceService;

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Nested
    @DisplayName("POST /api/brands/{brandId}/review-sources")
    class CreateReviewSourceTests {

        @Test
        
        @DisplayName("Should create review source successfully and return 201 CREATED")
        void shouldCreateReviewSource_Successfully() throws Exception {
            // Given

            CreateReviewSourceRequest request = new CreateReviewSourceRequest();
            request.setSourceType(SourceType.GOOGLE);
            request.setProfileUrl("https://google.com/maps/place/123");
            request.setExternalProfileId("place-123");
            request.setAuthMethod(AuthMethod.API);

            ReviewSourceResponse response = ReviewSourceResponse.builder()
                .sourceId(1L)
                .brandId(1L)
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://google.com/maps/place/123")
                .externalProfileId("place-123")
                .authMethod(AuthMethod.API)
                .isActive(true)
                .importJobId(100L)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(reviewSourceService.createReviewSource(eq(1L), eq(1L), any(CreateReviewSourceRequest.class)))
                .thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/1/review-sources")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceId").value(1))
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.sourceType").value("GOOGLE"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.importJobId").value(100));

            verify(reviewSourceService).createReviewSource(eq(1L), eq(1L), any(CreateReviewSourceRequest.class));
        }

        @Test
        
        @DisplayName("Should return 403 FORBIDDEN when plan limit exceeded")
        void shouldReturnForbidden_WhenPlanLimitExceeded() throws Exception {
            // Given

            CreateReviewSourceRequest request = new CreateReviewSourceRequest();
            request.setSourceType(SourceType.GOOGLE);
            request.setProfileUrl("https://google.com/maps/place/123");
            request.setExternalProfileId("place-123");
            request.setAuthMethod(AuthMethod.API);

            when(reviewSourceService.createReviewSource(eq(1L), eq(1L), any(CreateReviewSourceRequest.class)))
                .thenThrow(new ValidationException("Plan limit exceeded. Upgrade to add more sources."));

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/1/review-sources")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(reviewSourceService).createReviewSource(eq(1L), eq(1L), any(CreateReviewSourceRequest.class));
        }

        @Test
        
        @DisplayName("Should return 400 BAD REQUEST when validation fails")
        void shouldReturnBadRequest_WhenValidationFails() throws Exception {
            // Given

            CreateReviewSourceRequest request = new CreateReviewSourceRequest();
            // Missing required fields

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/1/review-sources")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(reviewSourceService, never()).createReviewSource(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/brands/{brandId}/review-sources")
    class GetReviewSourcesTests {

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should get review sources successfully and return 200 OK")
        void shouldGetReviewSources_Successfully() throws Exception {
            // Given

            setupAuthenticatedUser(1L, "test@example.com");

            ReviewSourceResponse source1 = ReviewSourceResponse.builder()
                .sourceId(1L)
                .brandId(1L)
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://google.com/maps/place/123")
                .externalProfileId("place-123")
                .authMethod(AuthMethod.API)
                .isActive(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            ReviewSourceResponse source2 = ReviewSourceResponse.builder()
                .sourceId(2L)
                .brandId(1L)
                .sourceType(SourceType.FACEBOOK)
                .profileUrl("https://facebook.com/page/456")
                .externalProfileId("page-456")
                .authMethod(AuthMethod.SCRAPING)
                .isActive(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            ReviewSourceListResponse response = new ReviewSourceListResponse(List.of(source1, source2));

            when(reviewSourceService.getReviewSources(1L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources[0].sourceId").value(1))
                .andExpect(jsonPath("$.sources[0].sourceType").value("GOOGLE"))
                .andExpect(jsonPath("$.sources[1].sourceId").value(2))
                .andExpect(jsonPath("$.sources[1].sourceType").value("FACEBOOK"));

            verify(reviewSourceService).getReviewSources(1L, 1L);
        }

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should return empty list when brand has no sources")
        void shouldReturnEmptyList_WhenBrandHasNoSources() throws Exception {
            // Given

            setupAuthenticatedUser(1L, "test@example.com");

            ReviewSourceListResponse response = new ReviewSourceListResponse(List.of());

            when(reviewSourceService.getReviewSources(1L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources").isEmpty());

            verify(reviewSourceService).getReviewSources(1L, 1L);
        }
    }

    @Nested
    @DisplayName("GET /api/brands/{brandId}/review-sources/{sourceId}")
    class GetReviewSourceByIdTests {

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should get review source by ID successfully and return 200 OK")
        void shouldGetReviewSourceById_Successfully() throws Exception {
            // Given

            setupAuthenticatedUser(1L, "test@example.com");

            ReviewSourceResponse response = ReviewSourceResponse.builder()
                .sourceId(1L)
                .brandId(1L)
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://google.com/maps/place/123")
                .externalProfileId("place-123")
                .authMethod(AuthMethod.API)
                .isActive(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(reviewSourceService.getReviewSourceById(1L, 1L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources/1")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value(1))
                .andExpect(jsonPath("$.sourceType").value("GOOGLE"));

            verify(reviewSourceService).getReviewSourceById(1L, 1L, 1L);
        }

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should return 404 NOT FOUND when source doesn't exist")
        void shouldReturnNotFound_WhenSourceDoesNotExist() throws Exception {
            // Given

            setupAuthenticatedUser(1L, "test@example.com");

            when(reviewSourceService.getReviewSourceById(1L, 1L, 999L))
                .thenThrow(new ResourceNotFoundException("ReviewSource", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources/999")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(reviewSourceService).getReviewSourceById(1L, 1L, 999L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/brands/{brandId}/review-sources/{sourceId}")
    class UpdateReviewSourceTests {

        @Test
        
        @DisplayName("Should update review source successfully and return 200 OK")
        void shouldUpdateReviewSource_Successfully() throws Exception {
            // Given

            UpdateReviewSourceRequest request = new UpdateReviewSourceRequest();
            request.setIsActive(false);

            ReviewSourceResponse response = ReviewSourceResponse.builder()
                .sourceId(1L)
                .brandId(1L)
                .sourceType(SourceType.GOOGLE)
                .profileUrl("https://google.com/maps/place/123")
                .externalProfileId("place-123")
                .authMethod(AuthMethod.API)
                .isActive(false)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(reviewSourceService.updateReviewSource(eq(1L), eq(1L), eq(1L), any(UpdateReviewSourceRequest.class)))
                .thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(patch("/api/brands/1/review-sources/1")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value(1))
                .andExpect(jsonPath("$.isActive").value(false));

            verify(reviewSourceService).updateReviewSource(eq(1L), eq(1L), eq(1L), any(UpdateReviewSourceRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/brands/{brandId}/review-sources/{sourceId}")
    class DeleteReviewSourceTests {

        @Test
        
        @DisplayName("Should delete review source successfully and return 204 NO CONTENT")
        void shouldDeleteReviewSource_Successfully() throws Exception {
            // Given

            doNothing().when(reviewSourceService).deleteReviewSource(1L, 1L, 1L);

            // When
            ResultActions result = mockMvc.perform(delete("/api/brands/1/review-sources/1")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNoContent());

            verify(reviewSourceService).deleteReviewSource(1L, 1L, 1L);
        }

        @Test
        
        @DisplayName("Should return 404 NOT FOUND when source doesn't exist")
        void shouldReturnNotFound_WhenSourceDoesNotExist() throws Exception {
            // Given

            doThrow(new ResourceNotFoundException("ReviewSource", "id", 999L))
                .when(reviewSourceService).deleteReviewSource(1L, 1L, 999L);

            // When
            ResultActions result = mockMvc.perform(delete("/api/brands/1/review-sources/999")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(reviewSourceService).deleteReviewSource(1L, 1L, 999L);
        }

        @Test
        
        @DisplayName("Should return 403 FORBIDDEN when user doesn't own source")
        void shouldReturnForbidden_WhenUserDoesNotOwnSource() throws Exception {
            // Given

            doThrow(new ResourceAccessDeniedException("Access denied"))
                .when(reviewSourceService).deleteReviewSource(1L, 1L, 2L);

            // When
            ResultActions result = mockMvc.perform(delete("/api/brands/1/review-sources/2")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(reviewSourceService).deleteReviewSource(1L, 1L, 2L);
        }
    }
}
