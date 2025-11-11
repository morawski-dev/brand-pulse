package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.dto.user.UpdateUserProfileRequest;
import com.morawski.dev.backend.dto.user.UserProfileResponse;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.service.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController using @WebMvcTest.
 * Tests user profile management endpoints with MockMvc.
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController Integration Tests")
class UserControllerTest extends ControllerTestBase {

    @MockBean
    private UserService userService;

    @AfterEach
    void tearDown() {
        clearSecurityContext();
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user profile successfully and return 200 OK")
        void shouldGetCurrentUserProfile_Successfully() throws Exception {
            // Given
            UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("test@example.com")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(userService.getCurrentUserProfile(1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me")
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.planType").value("FREE"))
                .andExpect(jsonPath("$.maxSourcesAllowed").value(1))
                .andExpect(jsonPath("$.emailVerified").value(true));

            verify(userService).getCurrentUserProfile(1L);
        }

        @Test
        @DisplayName("Should get premium user profile successfully")
        void shouldGetPremiumUserProfile_Successfully() throws Exception {
            // Given
            UserProfileResponse response = UserProfileResponse.builder()
                .userId(2L)
                .email("premium@example.com")
                .planType(PlanType.PREMIUM)
                .maxSourcesAllowed(10)
                .emailVerified(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(userService.getCurrentUserProfile(2L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me")
                .with(authenticatedUser(2L, "premium@example.com", PlanType.PREMIUM, 10))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.planType").value("PREMIUM"))
                .andExpect(jsonPath("$.maxSourcesAllowed").value(10));

            verify(userService).getCurrentUserProfile(2L);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(userService, never()).getCurrentUserProfile(any());
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when user not found")
        void shouldReturnNotFound_WhenUserNotFound() throws Exception {
            // Given
            when(userService.getCurrentUserProfile(999L))
                .thenThrow(new ResourceNotFoundException("User", "id", 999L));

            // When
            ResultActions result = mockMvc.perform(get("/api/users/me")
                .with(authenticatedUser(999L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isNotFound());

            verify(userService).getCurrentUserProfile(999L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me")
    class UpdateCurrentUserTests {

        @Test
        @DisplayName("Should update user profile successfully and return 200 OK")
        void shouldUpdateUserProfile_Successfully() throws Exception {
            // Given
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("newemail@example.com");

            UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("newemail@example.com")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(false) // Email verification required after email change
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

            when(userService.updateUserProfile(eq(1L), any(UpdateUserProfileRequest.class)))
                .thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(patch("/api/users/me")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("newemail@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(false));

            verify(userService).updateUserProfile(eq(1L), any(UpdateUserProfileRequest.class));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when email format is invalid")
        void shouldReturnBadRequest_WhenEmailInvalid() throws Exception {
            // Given
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("invalid-email");

            // When
            ResultActions result = mockMvc.perform(patch("/api/users/me")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(userService, never()).updateUserProfile(any(), any());
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email is already in use")
        void shouldReturnConflict_WhenEmailAlreadyInUse() throws Exception {
            // Given
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("existing@example.com");

            when(userService.updateUserProfile(eq(1L), any(UpdateUserProfileRequest.class)))
                .thenThrow(new ValidationException("Email address is already in use"));

            // When
            ResultActions result = mockMvc.perform(patch("/api/users/me")
                .with(csrf())
                .with(authenticatedUser(1L, "test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(userService).updateUserProfile(eq(1L), any(UpdateUserProfileRequest.class));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // Given
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("newemail@example.com");

            // When
            ResultActions result = mockMvc.perform(patch("/api/users/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(userService, never()).updateUserProfile(any(), any());
        }
    }
}
