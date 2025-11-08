package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.auth.*;
import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.*;
import com.morawski.dev.backend.mapper.UserMapper;
import com.morawski.dev.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private final String testToken = "test.jwt.token";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .passwordHash("$2a$10$hashedPassword")
            .planType(PlanType.FREE)
            .maxSourcesAllowed(1)
            .emailVerified(false)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("register() Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser_Successfully() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("Password123!");
            request.setConfirmPassword("Password123!");

            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
            when(userService.createUser(anyString(), anyString())).thenReturn(testUser);
            when(jwtTokenProvider.generateToken(any(User.class))).thenReturn(testToken);
            when(jwtTokenProvider.getTokenExpiration()).thenReturn(Instant.now().plusSeconds(3600));

            // When
            AuthResponse response = authService.register(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(testToken);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getPlanType()).isEqualTo(PlanType.FREE);

            verify(passwordEncoder).encode("Password123!");
            verify(userService).createUser("newuser@example.com", "$2a$10$hashedPassword");
            verify(jwtTokenProvider).generateToken(testUser);
        }

        @Test
        @DisplayName("Should throw ValidationException when passwords don't match")
        void shouldThrowException_WhenPasswordsDontMatch() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("Password123!");
            request.setConfirmPassword("DifferentPassword123!");

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Passwords do not match");

            verify(userService, never()).createUser(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw ValidationException when password is weak")
        void shouldThrowException_WhenPasswordIsWeak() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("weak");
            request.setConfirmPassword("weak");

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password must contain");

            verify(userService, never()).createUser(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("login() Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void shouldLoginUser_WithValidCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");

            when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password123!", testUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(testUser)).thenReturn(testToken);
            when(jwtTokenProvider.getTokenExpiration()).thenReturn(Instant.now().plusSeconds(3600));

            // When
            AuthResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(testToken);
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test@example.com");

            verify(userService).findByEmail("test@example.com");
            verify(passwordEncoder).matches("Password123!", testUser.getPasswordHash());
            verify(jwtTokenProvider).generateToken(testUser);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user not found")
        void shouldThrowException_WhenUserNotFound() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("nonexistent@example.com");
            request.setPassword("Password123!");

            when(userService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
        void shouldThrowException_WhenPasswordIncorrect() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("WrongPassword123!");

            when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPassword123!", testUser.getPasswordHash())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

            verify(jwtTokenProvider, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("forgotPassword() Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should process forgot password for existing user")
        void shouldProcessForgotPassword_ForExistingUser() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("test@example.com");

            when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            doNothing().when(userService).setPasswordResetToken(anyLong(), anyString(), any(Instant.class));

            // When
            AuthService.MessageResponse response = authService.forgotPassword(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.message()).contains("password reset link has been sent");

            verify(userService).findByEmail("test@example.com");
            verify(userService).setPasswordResetToken(eq(1L), anyString(), any(Instant.class));
        }

        @Test
        @DisplayName("Should return success message even when user doesn't exist")
        void shouldReturnSuccessMessage_WhenUserDoesNotExist() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("nonexistent@example.com");

            when(userService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When
            AuthService.MessageResponse response = authService.forgotPassword(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.message()).contains("password reset link has been sent");

            verify(userService).findByEmail("nonexistent@example.com");
            verify(userService, never()).setPasswordResetToken(anyLong(), anyString(), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("resetPassword() Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password with valid token")
        void shouldResetPassword_WithValidToken() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("NewPassword123!");

            testUser.setPasswordResetToken("valid-reset-token");
            testUser.setPasswordResetExpiresAt(Instant.now().plusSeconds(3600));

            when(userService.findByValidPasswordResetToken("valid-reset-token"))
                .thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("NewPassword123!")).thenReturn("$2a$10$newHashedPassword");
            doNothing().when(userService).updatePassword(eq(1L), anyString());
            doNothing().when(userService).clearPasswordResetToken(1L);

            // When
            AuthService.MessageResponse response = authService.resetPassword(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.message()).contains("Password reset successfully");

            verify(userService).findByValidPasswordResetToken("valid-reset-token");
            verify(passwordEncoder).encode("NewPassword123!");
            verify(userService).updatePassword(1L, "$2a$10$newHashedPassword");
            verify(userService).clearPasswordResetToken(1L);
        }

        @Test
        @DisplayName("Should throw ValidationException when passwords don't match")
        void shouldThrowException_WhenPasswordsDontMatch() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("DifferentPassword123!");

            // When & Then
            assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Passwords do not match");

            verify(userService, never()).findByValidPasswordResetToken(anyString());
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is invalid")
        void shouldThrowException_WhenTokenInvalid() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("invalid-token");
            request.setNewPassword("NewPassword123!");
            request.setConfirmPassword("NewPassword123!");

            when(userService.findByValidPasswordResetToken("invalid-token"))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Password reset token is invalid or has expired");

            verify(userService, never()).updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should throw ValidationException when password is weak")
        void shouldThrowException_WhenPasswordIsWeak() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("weak");
            request.setConfirmPassword("weak");

            // When & Then
            assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Password must contain");

            verify(userService, never()).findByValidPasswordResetToken(anyString());
        }
    }

    @Nested
    @DisplayName("logout() Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully")
        void shouldLogoutUser_Successfully() {
            // Given
            Long userId = 1L;
            when(userService.findByIdOrThrow(userId)).thenReturn(testUser);

            // When
            AuthService.MessageResponse response = authService.logout(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.message()).contains("Logged out successfully");

            verify(userService).findByIdOrThrow(userId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowException_WhenUserNotFound() {
            // Given
            Long userId = 999L;
            when(userService.findByIdOrThrow(userId))
                .thenThrow(new ResourceNotFoundException("User", "id", userId));

            // When & Then
            assertThatThrownBy(() -> authService.logout(userId))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(userService).findByIdOrThrow(userId);
        }
    }
}
