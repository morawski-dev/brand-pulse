package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import com.morawski.dev.backend.dto.sync.*;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SyncJob;
import com.morawski.dev.backend.exception.RateLimitExceededException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.SyncInProgressException;
import com.morawski.dev.backend.mapper.SyncJobMapper;
import com.morawski.dev.backend.repository.SyncJobRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncJobService Tests")
class SyncJobServiceTest {

    @Mock
    private SyncJobRepository syncJobRepository;

    @Mock
    private ReviewSourceService reviewSourceService;

    @Mock
    private BrandService brandService;

    @Mock
    private SyncJobMapper syncJobMapper;

    @InjectMocks
    private SyncJobService syncJobService;

    private Brand testBrand;
    private ReviewSource testReviewSource;
    private SyncJob testSyncJob;

    @BeforeEach
    void setUp() {
        testBrand = Brand.builder()
            .id(1L)
            .name("Test Brand")
            .build();

        testReviewSource = ReviewSource.builder()
            .id(1L)
            .brand(testBrand)
            .build();

        testSyncJob = SyncJob.builder()
            .id(1L)
            .jobType(JobType.MANUAL)
            .status(JobStatus.PENDING)
            .reviewSource(testReviewSource)
            .createdAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("triggerManualSync() Tests")
    class TriggerManualSyncTests {

        @Test
        @DisplayName("Should trigger manual sync successfully")
        void shouldTriggerManualSync_Successfully() {
            // Given
            SyncJobResponse syncJobResponse = SyncJobResponse.builder()
                .jobId(1L)
                .status(JobStatus.PENDING)
                .jobType(JobType.MANUAL)
                .build();

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(syncJobRepository.findMostRecentByReviewSourceId(1L)).thenReturn(Optional.empty());
            when(syncJobRepository.existsActiveJobForSource(1L)).thenReturn(false);
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);
            when(syncJobMapper.toSyncJobResponse(any(SyncJob.class))).thenReturn(syncJobResponse);

            // When
            TriggerSyncResponse response = syncJobService.triggerManualSync(1L, 1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getJobs()).isNotEmpty();
            assertThat(response.getJobs().get(0).getJobId()).isEqualTo(1L);
            assertThat(response.getJobs().get(0).getStatus()).isEqualTo(JobStatus.PENDING);

            ArgumentCaptor<SyncJob> jobCaptor = ArgumentCaptor.forClass(SyncJob.class);
            verify(syncJobRepository).save(jobCaptor.capture());
            SyncJob savedJob = jobCaptor.getValue();
            assertThat(savedJob.getJobType()).isEqualTo(JobType.MANUAL);
            assertThat(savedJob.getStatus()).isEqualTo(JobStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw RateLimitExceededException when cooldown not expired")
        void shouldThrowException_WhenCooldownNotExpired() {
            // Given
            SyncJob recentJob = SyncJob.builder()
                .id(2L)
                .jobType(JobType.MANUAL)
                .status(JobStatus.COMPLETED)
                .reviewSource(testReviewSource)
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS)) // Less than 24 hours ago
                .build();

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(syncJobRepository.findMostRecentByReviewSourceId(1L)).thenReturn(Optional.of(recentJob));

            // When & Then
            assertThatThrownBy(() -> syncJobService.triggerManualSync(1L, 1L, 1L))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Manual refresh allowed once per 24 hours");

            verify(syncJobRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow manual sync when previous job was not manual")
        void shouldAllowManualSync_WhenPreviousJobNotManual() {
            // Given
            SyncJob scheduledJob = SyncJob.builder()
                .id(2L)
                .jobType(JobType.SCHEDULED)
                .status(JobStatus.COMPLETED)
                .reviewSource(testReviewSource)
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

            SyncJobResponse syncJobResponse = SyncJobResponse.builder()
                .jobId(1L)
                .status(JobStatus.PENDING)
                .jobType(JobType.MANUAL)
                .build();

            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(syncJobRepository.findMostRecentByReviewSourceId(1L)).thenReturn(Optional.of(scheduledJob));
            when(syncJobRepository.existsActiveJobForSource(1L)).thenReturn(false);
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);
            when(syncJobMapper.toSyncJobResponse(any(SyncJob.class))).thenReturn(syncJobResponse);

            // When
            TriggerSyncResponse response = syncJobService.triggerManualSync(1L, 1L, 1L);

            // Then
            assertThat(response).isNotNull();
            verify(syncJobRepository).save(any(SyncJob.class));
        }

        @Test
        @DisplayName("Should throw SyncInProgressException when job already active")
        void shouldThrowException_WhenJobActive() {
            // Given
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(reviewSourceService.findByIdAndBrandIdOrThrow(1L, 1L)).thenReturn(testReviewSource);
            when(syncJobRepository.findMostRecentByReviewSourceId(1L)).thenReturn(Optional.empty());
            when(syncJobRepository.existsActiveJobForSource(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> syncJobService.triggerManualSync(1L, 1L, 1L))
                .isInstanceOf(SyncInProgressException.class);

            verify(syncJobRepository, never()).save(any(SyncJob.class));
        }
    }

    @Nested
    @DisplayName("createInitialImportJob() Tests")
    class CreateInitialImportJobTests {

        @Test
        @DisplayName("Should create initial import job successfully")
        void shouldCreateInitialImportJob_Successfully() {
            // Given
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);

            // When
            SyncJob result = syncJobService.createInitialImportJob(testReviewSource);

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<SyncJob> jobCaptor = ArgumentCaptor.forClass(SyncJob.class);
            verify(syncJobRepository).save(jobCaptor.capture());
            SyncJob savedJob = jobCaptor.getValue();
            assertThat(savedJob.getJobType()).isEqualTo(JobType.INITIAL);
            assertThat(savedJob.getStatus()).isEqualTo(JobStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("createScheduledSyncJob() Tests")
    class CreateScheduledSyncJobTests {

        @Test
        @DisplayName("Should create scheduled sync job successfully")
        void shouldCreateScheduledSyncJob_Successfully() {
            // Given
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(
                SyncJob.builder()
                    .id(2L)
                    .jobType(JobType.SCHEDULED)
                    .status(JobStatus.PENDING)
                    .reviewSource(testReviewSource)
                    .build()
            );

            // When
            SyncJob result = syncJobService.createScheduledSyncJob(testReviewSource);

            // Then
            assertThat(result).isNotNull();
            verify(syncJobRepository).save(any(SyncJob.class));
        }
    }

    @Nested
    @DisplayName("hasActiveJobForSource() Tests")
    class HasActiveJobForSourceTests {

        @Test
        @DisplayName("Should return true when active job exists")
        void shouldReturnTrue_WhenActiveJobExists() {
            when(syncJobRepository.existsActiveJobForSource(1L)).thenReturn(true);

            boolean result = syncJobService.hasActiveJobForSource(1L);

            assertThat(result).isTrue();
            verify(syncJobRepository).existsActiveJobForSource(1L);
        }

        @Test
        @DisplayName("Should return false when no active job exists")
        void shouldReturnFalse_WhenNoActiveJobExists() {
            when(syncJobRepository.existsActiveJobForSource(1L)).thenReturn(false);

            boolean result = syncJobService.hasActiveJobForSource(1L);

            assertThat(result).isFalse();
            verify(syncJobRepository).existsActiveJobForSource(1L);
        }
    }

    @Nested
    @DisplayName("getSyncJobStatus() Tests")
    class GetSyncJobStatusTests {

        @Test
        @DisplayName("Should return sync job status")
        void shouldReturnSyncJobStatus() {
            // Given
            SyncJobStatusResponse statusResponse = SyncJobStatusResponse.builder()
                .jobId(1L)
                .status(JobStatus.PENDING)
                .build();

            when(syncJobRepository.findById(1L)).thenReturn(Optional.of(testSyncJob));
            when(brandService.findByIdAndUserIdOrThrow(1L, 1L)).thenReturn(testBrand);
            when(syncJobMapper.toSyncJobStatusResponse(testSyncJob)).thenReturn(statusResponse);

            // When
            SyncJobStatusResponse response = syncJobService.getSyncJobStatus(1L, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getJobId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(JobStatus.PENDING);

            verify(syncJobRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when job not found")
        void shouldThrowException_WhenJobNotFound() {
            // Given
            when(syncJobRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> syncJobService.getSyncJobStatus(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Job State Management Tests")
    class JobStateManagementTests {

        @Test
        @DisplayName("Should mark job as started")
        void shouldMarkJobAsStarted() {
            // Given
            when(syncJobRepository.findById(1L)).thenReturn(Optional.of(testSyncJob));
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);

            // When
            syncJobService.markJobAsStarted(1L);

            // Then
            ArgumentCaptor<SyncJob> jobCaptor = ArgumentCaptor.forClass(SyncJob.class);
            verify(syncJobRepository).save(jobCaptor.capture());
            SyncJob savedJob = jobCaptor.getValue();
            assertThat(savedJob.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
            assertThat(savedJob.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark job as completed")
        void shouldMarkJobAsCompleted() {
            // Given
            testSyncJob.setStartedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
            when(syncJobRepository.findById(1L)).thenReturn(Optional.of(testSyncJob));
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);

            // When
            syncJobService.markJobAsCompleted(1L);

            // Then
            ArgumentCaptor<SyncJob> jobCaptor = ArgumentCaptor.forClass(SyncJob.class);
            verify(syncJobRepository).save(jobCaptor.capture());
            SyncJob savedJob = jobCaptor.getValue();
            assertThat(savedJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(savedJob.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should mark job as failed")
        void shouldMarkJobAsFailed() {
            // Given
            when(syncJobRepository.findById(1L)).thenReturn(Optional.of(testSyncJob));
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);

            // When
            syncJobService.markJobAsFailed(1L, "Test error message");

            // Then
            ArgumentCaptor<SyncJob> jobCaptor = ArgumentCaptor.forClass(SyncJob.class);
            verify(syncJobRepository).save(jobCaptor.capture());
            SyncJob savedJob = jobCaptor.getValue();
            assertThat(savedJob.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(savedJob.getErrorMessage()).isEqualTo("Test error message");
            assertThat(savedJob.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should update job progress")
        void shouldUpdateJobProgress() {
            // Given
            when(syncJobRepository.findById(1L)).thenReturn(Optional.of(testSyncJob));
            when(syncJobRepository.save(any(SyncJob.class))).thenReturn(testSyncJob);

            // When
            syncJobService.updateJobProgress(1L, 100, 50, 25);

            // Then
            ArgumentCaptor<SyncJob> jobCaptor = ArgumentCaptor.forClass(SyncJob.class);
            verify(syncJobRepository).save(jobCaptor.capture());
            SyncJob savedJob = jobCaptor.getValue();
            assertThat(savedJob.getReviewsFetched()).isEqualTo(100);
            assertThat(savedJob.getReviewsNew()).isEqualTo(50);
            assertThat(savedJob.getReviewsUpdated()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("Should find pending jobs")
        void shouldFindPendingJobs() {
            // Given
            when(syncJobRepository.findPendingJobs()).thenReturn(List.of(testSyncJob));

            // When
            List<SyncJob> jobs = syncJobService.findPendingJobs();

            // Then
            assertThat(jobs).hasSize(1);
            assertThat(jobs.get(0).getStatus()).isEqualTo(JobStatus.PENDING);
        }

        @Test
        @DisplayName("Should find stuck jobs")
        void shouldFindStuckJobs() {
            // Given
            Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);
            when(syncJobRepository.findStuckJobs(any(Instant.class))).thenReturn(List.of(testSyncJob));

            // When
            List<SyncJob> jobs = syncJobService.findStuckJobs(30);

            // Then
            assertThat(jobs).hasSize(1);
            verify(syncJobRepository).findStuckJobs(any(Instant.class));
        }

        @Test
        @DisplayName("Should calculate average duration")
        void shouldCalculateAverageDuration() {
            // Given
            when(syncJobRepository.calculateAverageDuration(1L)).thenReturn(120.0);

            // When
            Double avgDuration = syncJobService.calculateAverageDuration(1L);

            // Then
            assertThat(avgDuration).isEqualTo(120.0);
        }

        @Test
        @DisplayName("Should count jobs by status")
        void shouldCountJobsByStatus() {
            // Given
            when(syncJobRepository.countByReviewSourceIdAndStatus(1L, JobStatus.COMPLETED)).thenReturn(5L);

            // When
            long count = syncJobService.countByStatus(1L, JobStatus.COMPLETED);

            // Then
            assertThat(count).isEqualTo(5L);
        }
    }
}
