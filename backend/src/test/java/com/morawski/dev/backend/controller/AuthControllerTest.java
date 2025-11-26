package com.morawski.dev.backend.controller;

import com.morawski.dev.backend.dto.auth.*;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.exception.InvalidCredentialsException;
import com.morawski.dev.backend.exception.InvalidTokenException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.security.test.WithMockCustomUser;
import com.morawski.dev.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController using @WebMvcTest.
 * Tests all authentication endpoints with MockMvc.
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest extends ControllerTestBase {

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully and return 201 CREATED")
        void shouldRegisterNewUser_Successfully() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("Password123!");
            request.setConfirmPassword("Password123!");

            AuthResponse response = AuthResponse.builder()
                .userId(1L)
                .email("newuser@example.com")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(false)
                .token("test.jwt.token")
                .expiresAt(ZonedDateTime.now().plusHours(1))
                .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.planType").value("FREE"))
                .andExpect(jsonPath("$.maxSourcesAllowed").value(1));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when validation fails")
        void shouldReturnBadRequest_WhenValidationFails() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("invalid-email");
            request.setPassword("weak");
            request.setConfirmPassword("weak");

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(authService, never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when passwords don't match")
        void shouldReturnBadRequest_WhenPasswordsDontMatch() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("Password123!");
            request.setConfirmPassword("DifferentPassword123!");

            when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ValidationException("Passwords do not match"));

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(authService).register(any(RegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully and return 200 OK")
        void shouldLoginUser_Successfully() throws Exception {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");

            AuthResponse response = AuthResponse.builder()
                .userId(1L)
                .email("test@example.com")
                .planType(PlanType.FREE)
                .maxSourcesAllowed(1)
                .emailVerified(true)
                .token("test.jwt.token")
                .expiresAt(ZonedDateTime.now().plusHours(1))
                .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value("test.jwt.token"));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when credentials are invalid")
        void shouldReturnUnauthorized_WhenCredentialsInvalid() throws Exception {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("WrongPassword");

            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when email is missing")
        void shouldReturnBadRequest_WhenEmailMissing() throws Exception {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPassword("Password123!");

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should process forgot password request and return 200 OK")
        void shouldProcessForgotPassword_Successfully() throws Exception {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("test@example.com");

            AuthService.MessageResponse response = new AuthService.MessageResponse(
                "If an account exists with that email, a password reset link has been sent."
            );

            when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

            verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
        }

        @Test
        @DisplayName("Should return success even when email doesn't exist")
        void shouldReturnSuccess_WhenEmailDoesNotExist() throws Exception {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("nonexistent@example.com");

            AuthService.MessageResponse response = new AuthService.MessageResponse(
                "If an account exists with that email, a password reset link has been sent."
            );

            when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk());

            verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password with valid token and return 200 OK")
        void shouldResetPassword_WithValidToken() throws Exception {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("NewPassword123!");

            AuthService.MessageResponse response = new AuthService.MessageResponse(
                "Password reset successfully. You can now log in with your new password."
            );

            when(authService.resetPassword(any(ResetPasswordRequest.class))).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

            verify(authService).resetPassword(any(ResetPasswordRequest.class));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when token is invalid")
        void shouldReturnUnauthorized_WhenTokenInvalid() throws Exception {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("invalid-token");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("NewPassword123!");

            when(authService.resetPassword(any(ResetPasswordRequest.class)))
                .thenThrow(new InvalidTokenException("Password reset token is invalid or has expired"));

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(authService).resetPassword(any(ResetPasswordRequest.class));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when passwords don't match")
        void shouldReturnBadRequest_WhenPasswordsDontMatch() throws Exception {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("DifferentPassword123!");

            when(authService.resetPassword(any(ResetPasswordRequest.class)))
                .thenThrow(new ValidationException("Passwords do not match"));

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

            // Then
            result.andExpect(status().isBadRequest());

            verify(authService).resetPassword(any(ResetPasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @WithMockCustomUser
        @DisplayName("Should logout user successfully and return 200 OK")
        void shouldLogoutUser_Successfully() throws Exception {
            // Given
            AuthService.MessageResponse response = new AuthService.MessageResponse(
                "Logged out successfully. Please delete the token from your local storage."
            );

            when(authService.logout(1L)).thenReturn(response);

            // When
            ResultActions result = mockMvc.perform(post("/api/auth/logout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

            verify(authService).logout(1L);
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when user is not authenticated")
        void shouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
            // When
            ResultActions result = mockMvc.perform(post("/api/auth/logout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());

            verify(authService, never()).logout(any());
        }
    }
}
