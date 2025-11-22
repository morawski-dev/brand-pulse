package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.activity.UserActivityListResponse;
import com.morawski.dev.backend.dto.activity.UserActivityResponse;
import com.morawski.dev.backend.dto.common.ActivityType;
import com.morawski.dev.backend.service.UserActivityService;
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
import java.util.Map;

import com.morawski.dev.backend.dto.common.PaginationResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserActivityController using @WebMvcTest.
 * Tests user activity log endpoints with MockMvc.
 */
@WebMvcTest(UserActivityController.class)
@DisplayName("UserActivityController Integration Tests")
class UserActivityControllerTest extends ControllerTestBase {

    @MockBean
    private UserActivityService userActivityService;

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Nested
    @DisplayName("GET /api/users/me/activity")
    class GetUserActivityTests {

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should get user activity successfully and return 200 OK")
        void shouldGetUserActivity_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            UserActivityResponse activity1 = UserActivityResponse.builder()
                .activityId(1L)
                .activityType(ActivityType.LOGIN)
                .occurredAt(ZonedDateTime.now())
                .metadata(Map.of("ip", "192.168.1.1"))
                .build();

            UserActivityResponse activity2 = UserActivityResponse.builder()
                .activityId(2L)
                .activityType(ActivityType.VIEW_DASHBOARD)
                .occurredAt(ZonedDateTime.now())
                .metadata(Map.of("brandId", "1"))
                .build();

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(2L)
                .totalPages(1)
                .build();

            UserActivityListResponse response = UserActivityListResponse.builder()
                .activities(List.of(activity1, activity2))
                .pagination(pagination)
                .build();

            when(userActivityService.getUserActivityHistory(1L, 0, 20)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me/activity")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.activities[0].activityId").value(1))
                .andExpect(jsonPath("$.activities[0].activityType").value("LOGIN"))
                .andExpect(jsonPath("$.activities[1].activityId").value(2))
                .andExpect(jsonPath("$.activities[1].activityType").value("VIEW_DASHBOARD"))
                .andExpect(jsonPath("$.pagination.currentPage").value(0))
                .andExpect(jsonPath("$.pagination.pageSize").value(20))
                .andExpect(jsonPath("$.pagination.totalItems").value(2))
                .andExpect(jsonPath("$.pagination.totalPages").value(1));

            verify(userActivityService).getUserActivityHistory(1L, 0, 20);
        }

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should get user activity with custom pagination successfully")
        void shouldGetUserActivityWithCustomPagination_Successfully() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(2)
                .pageSize(10)
                .totalItems(0L)
                .totalPages(0)
                .build();

            UserActivityListResponse response = UserActivityListResponse.builder()
                .activities(List.of())
                .pagination(pagination)
                .build();

            when(userActivityService.getUserActivityHistory(1L, 2, 10)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me/activity")
                .param("page", "2")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.pagination.pageSize").value(10));

            verify(userActivityService).getUserActivityHistory(1L, 2, 10);
        }

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should return empty list when user has no activity")
        void shouldReturnEmptyList_WhenUserHasNoActivity() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(0L)
                .totalPages(0)
                .build();

            UserActivityListResponse response = UserActivityListResponse.builder()
                .activities(List.of())
                .pagination(pagination)
                .build();

            when(userActivityService.getUserActivityHistory(1L, 0, 20)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me/activity")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.activities").isEmpty())
                .andExpect(jsonPath("$.pagination.totalItems").value(0));

            verify(userActivityService).getUserActivityHistory(1L, 0, 20);
        }

        @Test
        @WithMockUser(username = "test@example.com", roles = "USER")
        @DisplayName("Should handle various activity types")
        void shouldHandleVariousActivityTypes() throws Exception {
            // Given
            setupAuthenticatedUser(1L, "test@example.com");

            UserActivityResponse activity1 = UserActivityResponse.builder()
                .activityId(1L)
                .activityType(ActivityType.USER_REGISTERED)
                .occurredAt(ZonedDateTime.now())
                .metadata(Map.of())
                .build();
            UserActivityResponse activity2 = UserActivityResponse.builder()
                .activityId(2L)
                .activityType(ActivityType.LOGIN)
                .occurredAt(ZonedDateTime.now())
                .metadata(Map.of())
                .build();
            UserActivityResponse activity3 = UserActivityResponse.builder()
                .activityId(3L)
                .activityType(ActivityType.SOURCE_ADDED)
                .occurredAt(ZonedDateTime.now())
                .metadata(Map.of("sourceId", "1"))
                .build();
            UserActivityResponse activity4 = UserActivityResponse.builder()
                .activityId(4L)
                .activityType(ActivityType.SENTIMENT_CORRECTED)
                .occurredAt(ZonedDateTime.now())
                .metadata(Map.of("reviewId", "5"))
                .build();

            PaginationResponse pagination = PaginationResponse.builder()
                .currentPage(0)
                .pageSize(20)
                .totalItems(4L)
                .totalPages(1)
                .build();

            UserActivityListResponse response = UserActivityListResponse.builder()
                .activities(List.of(activity1, activity2, activity3, activity4))
                .pagination(pagination)
                .build();

            when(userActivityService.getUserActivityHistory(1L, 0, 20)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me/activity")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.activities[0].activityType").value("USER_REGISTERED"))
                .andExpect(jsonPath("$.activities[1].activityType").value("LOGIN"))
                .andExpect(jsonPath("$.activities[2].activityType").value("SOURCE_ADDED"))
                .andExpect(jsonPath("$.activities[3].activityType").value("SENTIMENT_CORRECTED"))
                .andExpect(jsonPath("$.pagination.totalItems").value(4));

            verify(userActivityService).getUserActivityHistory(1L, 0, 20);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/users/me/activity")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(userActivityService, never()).getUserActivityHistory(any(), any(), any());
        }
    }
}
