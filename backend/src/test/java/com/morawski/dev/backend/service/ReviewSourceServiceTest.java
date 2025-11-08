package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.AuthMethod;
import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.common.SyncStatus;
import com.morawski.dev.backend.dto.source.*;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.*;
import com.morawski.dev.backend.mapper.ReviewSourceMapper;
import com.morawski.dev.backend.repository.ReviewSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewSourceService Tests")
class ReviewSourceServiceTest {

    @Mock
    private ReviewSourceRepository reviewSourceRepository;

    @Mock
    private BrandService brandService;

    @Mock
    private UserService userService;

    @Mock
    private ReviewSourceMapper reviewSourceMapper;

    @InjectMocks
    private ReviewSourceService reviewSourceService;

    private User testUser;
    private Brand testBrand;
    private ReviewSource testReviewSource;
    private ReviewSourceResponse testReviewSourceResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .maxSourcesAllowed(1)
            .build();

        testBrand = Brand.builder()
            .id(1L)
            .name("Test Brand")
            .user(testUser)
            .build();

        testReviewSource = ReviewSource.builder()
            .id(1L)
            .sourceType(SourceType.GOOGLE)
            .profileUrl("https://g.page/test")
            .externalProfileId("google-123")
            .authMethod(AuthMethod.API)
            .isActive(true)
            .brand(testBrand)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        testReviewSourceResponse = ReviewSourceResponse.builder()
            .sourceId(1L)
            .sourceType(SourceType.GOOGLE)
            .profileUrl("https://g.page/test")
            .isActive(true)
            .build();
    }

    @Nested
    @DisplayName("createReviewSource() Tests")
    class CreateReviewSourceTests {

        @Test
        @DisplayName("Should create review source successfully")
        void shouldCreateReviewSource_Successfully() {
            // Given
            CreateReviewSourceRequest request = new CreateReviewSourceRequest();
            request.setSourceType(SourceType.GOOGLE);
            request.setProfileUrl("https://g.page/newtest");
            request.setExternalProfileId("google-456");
            request.setAuthMethod(AuthMethod.API);
            request.setCredentialsEncrypted(java.util.Map.of("apiKey", "encrypted-key-123"));

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(0L);
            when(reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(1L, SourceType.GOOGLE, "google-456"))
                .thenReturn(false);
            when(reviewSourceRepository.save(any(ReviewSource.class))).thenReturn(testReviewSource);
            when(reviewSourceMapper.toReviewSourceResponse(testReviewSource)).thenReturn(testReviewSourceResponse);

            // When
            ReviewSourceResponse response = reviewSourceService.createReviewSource(1L, 1L, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSourceId()).isEqualTo(1L);

            ArgumentCaptor<ReviewSource> sourceCaptor = ArgumentCaptor.forClass(ReviewSource.class);
            verify(reviewSourceRepository).save(sourceCaptor.capture());
            ReviewSource savedSource = sourceCaptor.getValue();
            assertThat(savedSource.getSourceType()).isEqualTo(SourceType.GOOGLE);
            assertThat(savedSource.getIsActive()).isTrue();
            assertThat(savedSource.getNextScheduledSyncAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw PlanLimitExceededException when plan limit reached")
        void shouldThrowException_WhenPlanLimitReached() {
            // Given
            CreateReviewSourceRequest request = new CreateReviewSourceRequest();
            request.setSourceType(SourceType.GOOGLE);
            request.setProfileUrl("https://g.page/test");
            request.setExternalProfileId("google-123");
            request.setAuthMethod(AuthMethod.API);

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(1L); // Already has 1 source

            // When & Then
            assertThatThrownBy(() -> reviewSourceService.createReviewSource(1L, 1L, request))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("FREE plan allows");

            verify(reviewSourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when source already exists")
        void shouldThrowException_WhenSourceAlreadyExists() {
            // Given
            CreateReviewSourceRequest request = new CreateReviewSourceRequest();
            request.setSourceType(SourceType.GOOGLE);
            request.setProfileUrl("https://g.page/test");
            request.setExternalProfileId("google-123");
            request.setAuthMethod(AuthMethod.API);

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(userService.findByIdOrThrow(1L)).thenReturn(testUser);
            when(reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(0L);
            when(reviewSourceRepository.existsByBrandIdAndSourceTypeAndExternalProfileId(1L, SourceType.GOOGLE, "google-123"))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> reviewSourceService.createReviewSource(1L, 1L, request))
                .isInstanceOf(DuplicateResourceException.class);

            verify(reviewSourceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("scheduleNextDailySync() Tests")
    class ScheduleNextDailySyncTests {

        @Test
        @DisplayName("Should schedule next daily sync at 3 AM CET")
        void shouldScheduleNextDailySync() {
            when(reviewSourceRepository.findById(1L)).thenReturn(Optional.of(testReviewSource));

            reviewSourceService.scheduleNextDailySync(1L);

            assertThat(testReviewSource.getNextScheduledSyncAt()).isNotNull();
            verify(reviewSourceRepository).save(testReviewSource);
        }
    }

    @Nested
    @DisplayName("getReviewSources() Tests")
    class GetReviewSourcesTests {

        @Test
        @DisplayName("Should return all review sources for brand")
        void shouldReturnAllReviewSources_ForBrand() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByBrandId(1L)).thenReturn(List.of(testReviewSource));
            when(reviewSourceMapper.toReviewSourceResponse(testReviewSource)).thenReturn(testReviewSourceResponse);

            // When
            ReviewSourceListResponse response = reviewSourceService.getReviewSources(1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSources()).hasSize(1);
            assertThat(response.getSources().get(0).getSourceId()).isEqualTo(1L);

            verify(reviewSourceRepository).findByBrandId(1L);
        }
    }

    @Nested
    @DisplayName("getReviewSourceById() Tests")
    class GetReviewSourceByIdTests {

        @Test
        @DisplayName("Should return review source by ID")
        void shouldReturnReviewSource_ById() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByIdAndBrandId(1L, 1L)).thenReturn(Optional.of(testReviewSource));
            when(reviewSourceMapper.toReviewSourceResponse(testReviewSource)).thenReturn(testReviewSourceResponse);

            // When
            ReviewSourceResponse response = reviewSourceService.getReviewSourceById(1L, 1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSourceId()).isEqualTo(1L);

            verify(reviewSourceRepository).findByIdAndBrandId(1L, 1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when source not found")
        void shouldThrowException_WhenSourceNotFound() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByIdAndBrandId(999L, 1L)).thenReturn(Optional.empty());
            when(reviewSourceRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> reviewSourceService.getReviewSourceById(1L, 999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(reviewSourceRepository).findByIdAndBrandId(999L, 1L);
        }
    }

    @Nested
    @DisplayName("updateReviewSource() Tests")
    class UpdateReviewSourceTests {

        @Test
        @DisplayName("Should update review source isActive status")
        void shouldUpdateReviewSource_IsActive() {
            // Given
            UpdateReviewSourceRequest request = new UpdateReviewSourceRequest();
            request.setIsActive(false);

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByIdAndBrandId(1L, 1L)).thenReturn(Optional.of(testReviewSource));
            when(reviewSourceRepository.save(any(ReviewSource.class))).thenReturn(testReviewSource);
            when(reviewSourceMapper.toReviewSourceResponse(testReviewSource)).thenReturn(testReviewSourceResponse);

            // When
            ReviewSourceResponse response = reviewSourceService.updateReviewSource(1L, 1L, 1L, request);

            // Then
            assertThat(response).isNotNull();

            ArgumentCaptor<ReviewSource> sourceCaptor = ArgumentCaptor.forClass(ReviewSource.class);
            verify(reviewSourceRepository).save(sourceCaptor.capture());
            ReviewSource savedSource = sourceCaptor.getValue();
            assertThat(savedSource.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should update review source URL")
        void shouldUpdateReviewSource_Url() {
            // Given
            UpdateReviewSourceRequest request = new UpdateReviewSourceRequest();
            request.setProfileUrl("https://g.page/updated");

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByIdAndBrandId(1L, 1L)).thenReturn(Optional.of(testReviewSource));
            when(reviewSourceRepository.save(any(ReviewSource.class))).thenReturn(testReviewSource);
            when(reviewSourceMapper.toReviewSourceResponse(testReviewSource)).thenReturn(testReviewSourceResponse);

            // When
            ReviewSourceResponse response = reviewSourceService.updateReviewSource(1L, 1L, 1L, request);

            // Then
            assertThat(response).isNotNull();

            ArgumentCaptor<ReviewSource> sourceCaptor = ArgumentCaptor.forClass(ReviewSource.class);
            verify(reviewSourceRepository).save(sourceCaptor.capture());
            ReviewSource savedSource = sourceCaptor.getValue();
            assertThat(savedSource.getProfileUrl()).isEqualTo("https://g.page/updated");
        }
    }

    @Nested
    @DisplayName("deleteReviewSource() Tests")
    class DeleteReviewSourceTests {

        @Test
        @DisplayName("Should soft delete review source")
        void shouldSoftDelete_ReviewSource() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceRepository.findByIdAndBrandId(1L, 1L)).thenReturn(Optional.of(testReviewSource));
            when(reviewSourceRepository.save(any(ReviewSource.class))).thenReturn(testReviewSource);

            // When
            reviewSourceService.deleteReviewSource(1L, 1L, 1L);

            // Then
            ArgumentCaptor<ReviewSource> sourceCaptor = ArgumentCaptor.forClass(ReviewSource.class);
            verify(reviewSourceRepository).save(sourceCaptor.capture());
            ReviewSource deletedSource = sourceCaptor.getValue();
            assertThat(deletedSource.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("Should update sync status successfully")
        void shouldUpdateSyncStatus_Successfully() {
            // Given
            when(reviewSourceRepository.findById(1L)).thenReturn(Optional.of(testReviewSource));
            when(reviewSourceRepository.save(any(ReviewSource.class))).thenReturn(testReviewSource);

            // When
            reviewSourceService.updateSyncStatus(1L, SyncStatus.SUCCESS, null);

            // Then
            ArgumentCaptor<ReviewSource> sourceCaptor = ArgumentCaptor.forClass(ReviewSource.class);
            verify(reviewSourceRepository).save(sourceCaptor.capture());
            ReviewSource savedSource = sourceCaptor.getValue();
            assertThat(savedSource.getLastSyncStatus()).isEqualTo(SyncStatus.SUCCESS);
            assertThat(savedSource.getLastSyncAt()).isNotNull();
        }

        @Test
        @DisplayName("Should find active sources")
        void shouldFindActiveSources() {
            // Given
            when(reviewSourceRepository.findByIsActiveTrue()).thenReturn(List.of(testReviewSource));

            // When
            List<ReviewSource> sources = reviewSourceService.findActiveSources();

            // Then
            assertThat(sources).hasSize(1);
            assertThat(sources.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should count active sources for brand")
        void shouldCountActiveSources_ForBrand() {
            // Given
            when(reviewSourceRepository.countByBrandIdAndDeletedAtIsNull(1L)).thenReturn(2L);

            // When
            long count = reviewSourceService.countActiveSourcesForBrand(1L);

            // Then
            assertThat(count).isEqualTo(2L);
        }
    }
}
