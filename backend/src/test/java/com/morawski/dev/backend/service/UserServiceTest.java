package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.user.*;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.DuplicateResourceException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.mapper.UserMapper;
import com.morawski.dev.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserProfileResponse testUserProfileResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .passwordHash("hashedPassword123")
            .emailVerified(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        testUserProfileResponse = UserProfileResponse.builder()
            .userId(1L)
            .email("test@example.com")
            .emailVerified(true)
            .build();
    }

    @Nested
    @DisplayName("getCurrentUserProfile() Tests")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("Should return user profile when user exists")
        void shouldReturnUserProfile_WhenUserExists() {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserProfileResponse(testUser)).thenReturn(testUserProfileResponse);

            // When
            UserProfileResponse result = userService.getCurrentUserProfile(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");

            verify(userRepository).findById(userId);
            verify(userMapper).toUserProfileResponse(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowException_WhenUserNotFound() {
            // Given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getCurrentUserProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("id")
                .hasMessageContaining("999");

            verify(userRepository).findById(userId);
            verify(userMapper, never()).toUserProfileResponse(any());
        }
    }

    @Nested
    @DisplayName("updateUserProfile() Tests")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Should update email and set emailVerified to false when email changed")
        void shouldUpdateEmailAndResetVerification_WhenEmailChanged() {
            // Given
            Long userId = 1L;
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("newemail@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailIgnoreCase("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserProfileResponse(any(User.class))).thenReturn(testUserProfileResponse);

            // When
            userService.updateUserProfile(userId, request);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo("newemail@example.com");
            assertThat(savedUser.getEmailVerified()).isFalse();
            verify(userRepository).existsByEmailIgnoreCase("newemail@example.com");
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when new email already exists")
        void shouldThrowException_WhenNewEmailAlreadyExists() {
            // Given
            Long userId = 1L;
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("existing@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.updateUserProfile(userId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not update email verification when email is same")
        void shouldNotUpdateEmailVerification_WhenEmailIsSame() {
            // Given
            Long userId = 1L;
            UpdateUserProfileRequest request = new UpdateUserProfileRequest();
            request.setEmail("test@example.com"); // Same as current email

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toUserProfileResponse(any(User.class))).thenReturn(testUserProfileResponse);

            // When
            userService.updateUserProfile(userId, request);

            // Then
            verify(userRepository, never()).existsByEmailIgnoreCase(anyString());
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmailVerified()).isTrue(); // Should remain unchanged
        }
    }

    @Nested
    @DisplayName("createUser() Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user with hashed password")
        void shouldCreateUser_WithHashedPassword() {
            // Given
            String email = "newuser@example.com";
            String hashedPassword = "hashedPassword123";

            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            User result = userService.createUser(email, hashedPassword);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo(email);
            assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword);
            assertThat(savedUser.getEmailVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByEmail() Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should return user when found by email")
        void shouldReturnUser_WhenFoundByEmail() {
            // Given
            String email = "test@example.com";
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findByEmail(email);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(email);
            verify(userRepository).findByEmailIgnoreCase(email);
        }

        @Test
        @DisplayName("Should return empty when user not found by email")
        void shouldReturnEmpty_WhenUserNotFoundByEmail() {
            // Given
            String email = "nonexistent@example.com";
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByEmail(email);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByEmailIgnoreCase(email);
        }
    }

    @Nested
    @DisplayName("findByIdOrThrow() Tests")
    class FindByIdOrThrowTests {

        @Test
        @DisplayName("Should return user when found by ID")
        void shouldReturnUser_WhenFoundById() {
            // Given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.findByIdOrThrow(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowException_WhenUserNotFound() {
            // Given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.findByIdOrThrow(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("id")
                .hasMessageContaining("999");

            verify(userRepository).findById(userId);
        }
    }
}
