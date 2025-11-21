package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.sync.*;
import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.service.SyncJobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;

import com.morawski.dev.backend.dto.common.PaginationResponse;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SyncJobController using @WebMvcTest.
 * Tests sync job management endpoints with MockMvc.
 */
@WebMvcTest(SyncJobController.class)
@DisplayName("SyncJobController Integration Tests")
class SyncJobControllerTest extends ControllerTestBase {

    @MockBean
    private SyncJobService syncJobService;

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }



    @Nested
    @DisplayName("POST /api/brands/{brandId}/sync")
    class TriggerManualSyncTests {

        @Test
        void shouldTriggerManualSync_Successfully() throws Exception {
            // Given
            long userId = 99L;
            long brandId = 42L;
            long sourceId = 123L;
            setupAuthenticatedUser(userId, "test@example.com");

            TriggerSyncRequest request = new TriggerSyncRequest();
            request.setSourceId(sourceId);

            TriggerSyncResponse response = TriggerSyncResponse.builder()
                .message("Sync triggered successfully")
                .jobs(List.of())
                .nextManualSyncAvailableAt(ZonedDateTime.now().plusDays(1))
                .build();

            when(syncJobService.triggerManualSync(brandId, sourceId, userId)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/" + brandId + "/sync")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Sync triggered successfully"))
                .andExpect(jsonPath("$.nextManualSyncAvailableAt").exists());

            verify(syncJobService).triggerManualSync(brandId, sourceId, userId);
        }

        @Test
        @DisplayName("Should trigger sync for all sources when sourceId is not provided")
        void shouldTriggerSyncForAllSources_Successfully() throws Exception {
            // Given
            long userId = 88L;
            long brandId = 55L;
            setupAuthenticatedUser(userId, "test@example.com");

            TriggerSyncResponse response = TriggerSyncResponse.builder()
                .message("Sync triggered for all sources")
                .jobs(List.of())
                .nextManualSyncAvailableAt(ZonedDateTime.now().plusDays(1))
                .build();

            when(syncJobService.triggerManualSync(brandId, null, userId)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/" + brandId + "/sync")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Sync triggered for all sources"))
                .andExpect(jsonPath("$.nextManualSyncAvailableAt").exists());

            verify(syncJobService).triggerManualSync(brandId, null, userId);
        }

        @Test
        @DisplayName("Should return 429 TOO MANY REQUESTS when rate limit exceeded")
        void shouldReturnTooManyRequests_WhenRateLimitExceeded() throws Exception {
            // Given
            long userId = 77L;
            long brandId = 66L;
            setupAuthenticatedUser(userId, "test@example.com");

            when(syncJobService.triggerManualSync(brandId, null, userId))
                .thenThrow(new ValidationException("Manual refresh already triggered in last 24 hours"));

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/" + brandId + "/sync")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isBadRequest());

            verify(syncJobService).triggerManualSync(brandId, null, userId);
        }

        @Test
        void shouldReturnForbidden_WhenUserDoesNotOwnBrand() throws Exception {
            // Given
            long userId = 45L;
            long brandId = 200L;
            setupAuthenticatedUser(userId, "test@example.com");

            when(syncJobService.triggerManualSync(brandId, null, userId))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(post("/api/brands/" + brandId + "/sync")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(syncJobService).triggerManualSync(brandId, null, userId);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(post("/api/brands/1/sync")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(syncJobService, never()).triggerManualSync(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/sync-jobs/{jobId}")
    class GetSyncJobStatusTests {

        @Test
        void shouldGetSyncJobStatus_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            SyncJobStatusResponse response = SyncJobStatusResponse.builder()
                .jobId(100L)
                .reviewSourceId(1L)
                .jobType(JobType.MANUAL)
                .status(JobStatus.COMPLETED)
                .reviewsFetched(50)
                .reviewsNew(30)
                .createdAt(ZonedDateTime.now())
                .startedAt(ZonedDateTime.now())
                .completedAt(ZonedDateTime.now())
                .build();

            when(syncJobService.getSyncJobStatus(100L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/sync-jobs/100")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(100))
                .andExpect(jsonPath("$.reviewSourceId").value(1))
                .andExpect(jsonPath("$.jobType").value("MANUAL"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reviewsFetched").value(50))
                .andExpect(jsonPath("$.reviewsNew").value(30));

            verify(syncJobService).getSyncJobStatus(100L, 1L);
        }

        @Test
        void shouldGetInProgressJobStatus_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            SyncJobStatusResponse response = SyncJobStatusResponse.builder()
                .jobId(100L)
                .reviewSourceId(1L)
                .jobType(JobType.INITIAL)
                .status(JobStatus.IN_PROGRESS)
                .reviewsFetched(20)
                .reviewsNew(15)
                .createdAt(ZonedDateTime.now())
                .startedAt(ZonedDateTime.now())
                .build();

            when(syncJobService.getSyncJobStatus(100L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/sync-jobs/100")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

            verify(syncJobService).getSyncJobStatus(100L, 1L);
        }

        @Test
        void shouldGetFailedJobStatus_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            SyncJobStatusResponse response = SyncJobStatusResponse.builder()
                .jobId(100L)
                .reviewSourceId(1L)
                .jobType(JobType.SCHEDULED)
                .status(JobStatus.FAILED)
                .reviewsFetched(0)
                .reviewsNew(0)
                .errorMessage("API rate limit exceeded")
                .createdAt(ZonedDateTime.now())
                .startedAt(ZonedDateTime.now())
                .completedAt(ZonedDateTime.now())
                .build();

            when(syncJobService.getSyncJobStatus(100L, 1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/sync-jobs/100")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("API rate limit exceeded"));

            verify(syncJobService).getSyncJobStatus(100L, 1L);
        }

        @Test
        void shouldReturnNotFound_WhenJobDoesNotExist() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            when(syncJobService.getSyncJobStatus(999L, 1L))
                .thenThrow(new ResourceNotFoundException("SyncJob", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/sync-jobs/999")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(syncJobService).getSyncJobStatus(999L, 1L);
        }

        @Test
        void shouldReturnForbidden_WhenUserDoesNotOwnJob() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            when(syncJobService.getSyncJobStatus(100L, 1L))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(get("/api/sync-jobs/100")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(syncJobService).getSyncJobStatus(100L, 1L);
        }
    }

    @Nested
    @DisplayName("GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs")
    class GetSyncJobsTests {

        @Test
        void shouldGetSyncJobs_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            SyncJobResponse job1 = SyncJobResponse.builder()
                .jobId(100L)
                .sourceId(1L)
                .jobType(JobType.INITIAL)
                .status(JobStatus.COMPLETED)
                .reviewsFetched(50)
                .reviewsNew(45)
                .startedAt(ZonedDateTime.now())
                .completedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

            SyncJobResponse job2 = SyncJobResponse.builder()
                .jobId(101L)
                .sourceId(1L)
                .jobType(JobType.SCHEDULED)
                .status(JobStatus.COMPLETED)
                .reviewsFetched(10)
                .reviewsNew(8)
                .startedAt(ZonedDateTime.now())
                .completedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .build();

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(2L)
                .totalPages(1)
                .build();

            SyncJobListResponse response = SyncJobListResponse.builder()
                .jobs(List.of(job1, job2))
                .pagination(pagination)
                .build();

            when(syncJobService.getSyncJobHistory(1L, 1L, 1L, 0, 20)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources/1/sync-jobs")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray())
                .andExpect(jsonPath("$.jobs[0].jobId").value(100))
                .andExpect(jsonPath("$.jobs[0].jobType").value("INITIAL"))
                .andExpect(jsonPath("$.jobs[1].jobId").value(101))
                .andExpect(jsonPath("$.jobs[1].jobType").value("SCHEDULED"))
                .andExpect(jsonPath("$.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.pagination.pageSize").value(20))
                .andExpect(jsonPath("$.pagination.totalItems").value(2));

            verify(syncJobService).getSyncJobHistory(1L, 1L, 1L, 0, 20);
        }

        @Test
        void shouldGetSyncJobsWithCustomPagination_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(1)
                .pageSize(10)
                .totalItems(0L)
                .totalPages(0)
                .build();

            SyncJobListResponse response = SyncJobListResponse.builder()
                .jobs(List.of())
                .pagination(pagination)
                .build();

            when(syncJobService.getSyncJobHistory(1L, 1L, 1L, 1, 10)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources/1/sync-jobs")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.pagination.pageSize").value(10));

            verify(syncJobService).getSyncJobHistory(1L, 1L, 1L, 1, 10);
        }

        @Test
        void shouldReturnNotFound_WhenSourceDoesNotExist() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            when(syncJobService.getSyncJobHistory(1L, 999L, 1L, 0, 20))
                .thenThrow(new ResourceNotFoundException("ReviewSource", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/1/review-sources/999/sync-jobs")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(syncJobService).getSyncJobHistory(1L, 999L, 1L, 0, 20);
        }

        @Test
        void shouldReturnForbidden_WhenUserDoesNotOwnSource() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            when(syncJobService.getSyncJobHistory(2L, 1L, 1L, 0, 20))
                .thenThrow(new ResourceAccessDeniedException("Access denied"));

            // When
            ResultActions result = mockMvc.perform(get("/api/brands/2/review-sources/1/sync-jobs")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isForbidden());

            verify(syncJobService).getSyncJobHistory(2L, 1L, 1L, 0, 20);
        }
    }
}
