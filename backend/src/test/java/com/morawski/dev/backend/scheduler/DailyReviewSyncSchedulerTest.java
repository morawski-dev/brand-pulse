package com.morawski.dev.backend.scheduler;

import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.SyncJob;
import com.morawski.dev.backend.service.ReviewSourceService;
import com.morawski.dev.backend.service.SyncJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyReviewSyncScheduler Tests")
class DailyReviewSyncSchedulerTest {

    @Mock
    private ReviewSourceService reviewSourceService;

    @Mock
    private SyncJobService syncJobService;

    @InjectMocks
    private DailyReviewSyncScheduler dailyReviewSyncScheduler;

    private ReviewSource googleSource;
    private SyncJob scheduledJob;

    @BeforeEach
    void setUp() {
        googleSource = ReviewSource.builder()
            .id(1L)
            .sourceType(SourceType.GOOGLE)
            .isActive(true)
            .build();

        scheduledJob = SyncJob.builder()
            .id(100L)
            .reviewSource(googleSource)
            .build();
    }

    @Nested
    @DisplayName("scheduleDailySyncJobs()")
    class ScheduleDailySyncJobs {

        @Test
        @DisplayName("should create scheduled job when source is ready and no active job")
        void shouldCreateScheduledJobWhenSourceReady() {
            when(reviewSourceService.findSourcesReadyForSync()).thenReturn(List.of(googleSource));
            when(syncJobService.hasActiveJobForSource(1L)).thenReturn(false);
            when(syncJobService.createScheduledSyncJob(googleSource)).thenReturn(scheduledJob);

            dailyReviewSyncScheduler.scheduleDailySyncJobs();

            verify(syncJobService).createScheduledSyncJob(googleSource);
            verify(reviewSourceService).scheduleNextDailySync(1L);
        }

        @Test
        @DisplayName("should skip source when job already active")
        void shouldSkipWhenJobActive() {
            when(reviewSourceService.findSourcesReadyForSync()).thenReturn(List.of(googleSource));
            when(syncJobService.hasActiveJobForSource(1L)).thenReturn(true);

            dailyReviewSyncScheduler.scheduleDailySyncJobs();

            verify(syncJobService, never()).createScheduledSyncJob(any());
            verify(reviewSourceService, never()).scheduleNextDailySync(anyLong());
        }

        @Test
        @DisplayName("should do nothing when no sources ready")
        void shouldDoNothingWhenNoSources() {
            when(reviewSourceService.findSourcesReadyForSync()).thenReturn(List.of());

            dailyReviewSyncScheduler.scheduleDailySyncJobs();

            verifyNoInteractions(syncJobService);
            verify(reviewSourceService, never()).scheduleNextDailySync(anyLong());
        }
    }
}
