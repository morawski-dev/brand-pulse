package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.activity.*;
import com.morawski.dev.backend.dto.common.ActivityType;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.entity.UserActivityLog;
import com.morawski.dev.backend.mapper.UserActivityLogMapper;
import com.morawski.dev.backend.repository.UserActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityService Tests")
class UserActivityServiceTest {

    @Mock
    private UserActivityLogRepository userActivityLogRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserActivityLogMapper userActivityLogMapper;

    @InjectMocks
    private UserActivityService userActivityService;

    private User testUser;
    private UserActivityLog testActivityLog;
    private UserActivityResponse testActivityResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .build();

        testActivityLog = UserActivityLog.builder()
            .id(1L)
            .activityType(ActivityType.LOGIN)
            .occurredAt(Instant.now())
            .user(testUser)
            .build();

        testActivityResponse = UserActivityResponse.builder()
            .activityId(1L)
            .activityType(ActivityType.LOGIN)
            .build();
    }

    @Nested
    @DisplayName("logActivity() Tests")
    class LogActivityTests {

        @Test
        @DisplayName("Should log activity successfully")
        void shouldLogActivity_Successfully() {
            // Given
            LogActivityRequest request = new LogActivityRequest();
            request.setActivityType(ActivityType.VIEW_DASHBOARD);
            request.setOccurredAt(Instant.now());

            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(userActivityLogRepository.save(any(UserActivityLog.class))).thenReturn(testActivityLog);
            when(userActivityLogMapper.toUserActivityResponse(testActivityLog)).thenReturn(testActivityResponse);

            // When
            UserActivityResponse response = userActivityService.logActivity(1L, request);

            // Then
            assertThat(response).isNotNull();

            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository).save(logCaptor.capture());
            UserActivityLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getActivityType()).isEqualTo(ActivityType.VIEW_DASHBOARD);
            assertThat(savedLog.getUser()).isEqualTo(testUser);
        }
    }

    @Nested
    @DisplayName("Specific Activity Logging Tests")
    class SpecificActivityLoggingTests {

        @Test
        @DisplayName("Should log registration activity")
        void shouldLogRegistration() {
            // Given
            when(userActivityLogRepository.save(any(UserActivityLog.class))).thenReturn(testActivityLog);

            // When
            userActivityService.logRegistration(testUser);

            // Then
            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository).save(logCaptor.capture());
            UserActivityLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getActivityType()).isEqualTo(ActivityType.USER_REGISTERED);
        }

        @Test
        @DisplayName("Should log login activity")
        void shouldLogLogin() {
            // Given
            when(userActivityLogRepository.save(any(UserActivityLog.class))).thenReturn(testActivityLog);

            // When
            userActivityService.logLogin(testUser);

            // Then
            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository).save(logCaptor.capture());
            UserActivityLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getActivityType()).isEqualTo(ActivityType.LOGIN);
        }

        @Test
        @DisplayName("Should log first source configured activity")
        void shouldLogFirstSourceConfigured() {
            // Given
            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(userActivityLogRepository.save(any(UserActivityLog.class))).thenReturn(testActivityLog);

            // When
            userActivityService.logFirstSourceConfigured(1L, 10L);

            // Then
            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository).save(logCaptor.capture());
            UserActivityLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getActivityType()).isEqualTo(ActivityType.FIRST_SOURCE_CONFIGURED_SUCCESSFULLY);
            assertThat(savedLog.getMetadata()).containsEntry("sourceId", 10L);
        }

        @Test
        @DisplayName("Should log sentiment correction activity")
        void shouldLogSentimentCorrection() {
            // Given
            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(userActivityLogRepository.save(any(UserActivityLog.class))).thenReturn(testActivityLog);

            // When
            userActivityService.logSentimentCorrection(1L, 100L, "POSITIVE", "NEGATIVE");

            // Then
            ArgumentCaptor<UserActivityLog> logCaptor = ArgumentCaptor.forClass(UserActivityLog.class);
            verify(userActivityLogRepository).save(logCaptor.capture());
            UserActivityLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getActivityType()).isEqualTo(ActivityType.SENTIMENT_CORRECTED);
            assertThat(savedLog.getMetadata()).containsEntry("reviewId", 100L);
            assertThat(savedLog.getMetadata()).containsEntry("oldSentiment", "POSITIVE");
            assertThat(savedLog.getMetadata()).containsEntry("newSentiment", "NEGATIVE");
        }
    }

    @Nested
    @DisplayName("Success Metrics Tests")
    class SuccessMetricsTests {

        @Test
        @DisplayName("Should calculate time to value")
        void shouldCalculateTimeToValue() {
            // Given
            Instant registrationTime = Instant.now().minus(5, ChronoUnit.MINUTES);
            Instant firstSourceTime = Instant.now();

            UserActivityLog registration = UserActivityLog.builder()
                .occurredAt(registrationTime)
                .build();

            UserActivityLog firstSource = UserActivityLog.builder()
                .occurredAt(firstSourceTime)
                .build();

            when(userActivityLogRepository.findRegistrationEvent(1L)).thenReturn(Optional.of(registration));
            when(userActivityLogRepository.findFirstSourceConfiguredEvent(1L)).thenReturn(Optional.of(firstSource));

            // When
            Duration timeToValue = userActivityService.calculateTimeToValue(1L);

            // Then
            assertThat(timeToValue).isNotNull();
            assertThat(timeToValue.toMinutes()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return null when no first source configured")
        void shouldReturnNull_WhenNoFirstSourceConfigured() {
            // Given
            when(userActivityLogRepository.findRegistrationEvent(1L)).thenReturn(Optional.of(testActivityLog));
            when(userActivityLogRepository.findFirstSourceConfiguredEvent(1L)).thenReturn(Optional.empty());

            // When
            Duration timeToValue = userActivityService.calculateTimeToValue(1L);

            // Then
            assertThat(timeToValue).isNull();
        }

        @Test
        @DisplayName("Should check time to value target achievement")
        void shouldCheckTimeToValueTarget() {
            // Given
            Instant registrationTime = Instant.now().minus(5, ChronoUnit.MINUTES);
            Instant firstSourceTime = Instant.now();

            UserActivityLog registration = UserActivityLog.builder()
                .occurredAt(registrationTime)
                .build();

            UserActivityLog firstSource = UserActivityLog.builder()
                .occurredAt(firstSourceTime)
                .build();

            when(userActivityLogRepository.findRegistrationEvent(1L)).thenReturn(Optional.of(registration));
            when(userActivityLogRepository.findFirstSourceConfiguredEvent(1L)).thenReturn(Optional.of(firstSource));

            // When
            boolean achieved = userActivityService.hasAchievedTimeToValueTarget(1L);

            // Then
            assertThat(achieved).isTrue();
        }

        @Test
        @DisplayName("Should check activation target achievement")
        void shouldCheckActivationTarget() {
            // Given
            Instant registrationTime = Instant.now().minus(3, ChronoUnit.DAYS);
            Instant firstSourceTime = Instant.now();

            UserActivityLog registration = UserActivityLog.builder()
                .occurredAt(registrationTime)
                .build();

            UserActivityLog firstSource = UserActivityLog.builder()
                .occurredAt(firstSourceTime)
                .build();

            when(userActivityLogRepository.findRegistrationEvent(1L)).thenReturn(Optional.of(registration));
            when(userActivityLogRepository.findFirstSourceConfiguredEvent(1L)).thenReturn(Optional.of(firstSource));

            // When
            boolean achieved = userActivityService.hasAchievedActivationTarget(1L);

            // Then
            assertThat(achieved).isTrue();
        }

        @Test
        @DisplayName("Should check retention target achievement")
        void shouldCheckRetentionTarget() {
            // Given
            Instant registrationTime = Instant.now().minus(14, ChronoUnit.DAYS);

            UserActivityLog registration = UserActivityLog.builder()
                .occurredAt(registrationTime)
                .build();

            when(userActivityLogRepository.findRegistrationEvent(1L)).thenReturn(Optional.of(registration));
            when(userActivityLogRepository.countLoginEvents(eq(1L), any(Instant.class), any(Instant.class)))
                .thenReturn(5L); // 5 logins >= 3 required

            // When
            boolean achieved = userActivityService.hasAchievedRetentionTarget(1L);

            // Then
            assertThat(achieved).isTrue();
        }
    }

    @Nested
    @DisplayName("getUserActivityHistory() Tests")
    class GetUserActivityHistoryTests {

        @Test
        @DisplayName("Should return user activity history")
        void shouldReturnUserActivityHistory() {
            // Given
            Page<UserActivityLog> activityPage = new PageImpl<>(List.of(testActivityLog));

            when(userActivityLogRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(activityPage);
            when(userActivityLogMapper.toUserActivityResponse(testActivityLog)).thenReturn(testActivityResponse);

            // When
            UserActivityListResponse response = userActivityService.getUserActivityHistory(1L, 0, 20);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getActivities()).hasSize(1);
            assertThat(response.getPagination().getTotalItems()).isEqualTo(1);
        }
    }
}
