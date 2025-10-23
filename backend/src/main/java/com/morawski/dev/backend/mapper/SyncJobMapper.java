package com.morawski.dev.backend.mapper;

import com.morawski.dev.backend.dto.sync.SyncJobResponse;
import com.morawski.dev.backend.dto.sync.SyncJobStatusResponse;
import com.morawski.dev.backend.entity.SyncJob;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Mapper for SyncJob entity to DTOs.
 */
@Component
public class SyncJobMapper implements BaseMapper {

    /**
     * Convert SyncJob entity to SyncJobStatusResponse DTO.
     *
     * @param entity SyncJob entity
     * @return SyncJobStatusResponse DTO
     */
    public SyncJobStatusResponse toSyncJobStatusResponse(SyncJob entity) {
        if (entity == null) {
            return null;
        }

        return SyncJobStatusResponse.builder()
            .jobId(entity.getId())
            .jobType(entity.getJobType())
            .status(entity.getStatus())
            .reviewSourceId(entity.getReviewSource().getId())
            .reviewsFetched(entity.getReviewsFetched())
            .reviewsNew(entity.getReviewsNew())
            .reviewsUpdated(entity.getReviewsUpdated())
            .createdAt(entity.getCreatedAt().atZone(ZoneId.of("UTC")))
            .startedAt(entity.getStartedAt() != null ? entity.getStartedAt().atZone(ZoneId.of("UTC")) : null)
            .completedAt(entity.getCompletedAt() != null ? entity.getCompletedAt().atZone(ZoneId.of("UTC")) : null)
            .duration(entity.getDuration())
            .errorMessage(entity.getErrorMessage())
            .build();
    }

    /**
     * Convert SyncJob entity to SyncJobResponse DTO.
     *
     * @param entity SyncJob entity
     * @return SyncJobResponse DTO
     */
    public SyncJobResponse toSyncJobResponse(SyncJob entity) {
        if (entity == null) {
            return null;
        }

        return SyncJobResponse.builder()
            .jobId(entity.getId())
            .jobType(entity.getJobType())
            .status(entity.getStatus())
            .sourceId(entity.getReviewSource().getId())
            .sourceType(entity.getReviewSource().getSourceType())
            .reviewsFetched(entity.getReviewsFetched())
            .reviewsNew(entity.getReviewsNew())
            .reviewsUpdated(entity.getReviewsUpdated())
            .createdAt(entity.getCreatedAt().atZone(ZoneId.of("UTC")))
            .startedAt(entity.getStartedAt() != null ? entity.getStartedAt().atZone(ZoneId.of("UTC")) : null)
            .completedAt(entity.getCompletedAt() != null ? entity.getCompletedAt().atZone(ZoneId.of("UTC")) : null)
            .errorMessage(entity.getErrorMessage())
            .build();
    }
}
