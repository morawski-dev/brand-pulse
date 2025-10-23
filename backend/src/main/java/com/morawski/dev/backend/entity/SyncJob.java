package com.morawski.dev.backend.entity;

import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.Instant;

/**
 * Entity representing a review synchronization job.
 * Tracks background jobs that import reviews from external sources (Google, Facebook, Trustpilot).
 *
 * Job Types:
 * - INITIAL: First-time import of last 90 days (triggered when source is created)
 * - SCHEDULED: Daily CRON job at 3:00 AM CET
 * - MANUAL: User-triggered refresh (24h cooldown)
 *
 * User Story US-008: Manual data refresh
 */
@Entity
@Table(name = "sync_jobs", indexes = {
        @Index(name = "idx_sync_jobs_source_id", columnList = "review_source_id"),
        @Index(name = "idx_sync_jobs_status", columnList = "status, created_at"),
        @Index(name = "idx_sync_jobs_type", columnList = "job_type, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Job type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @NotNull(message = "Job status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    /**
     * When the job actually started processing.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * When the job finished (successfully or with error).
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Total number of reviews fetched from external API.
     */
    @Min(value = 0, message = "Reviews fetched must be non-negative")
    @Column(name = "reviews_fetched")
    @Builder.Default
    private Integer reviewsFetched = 0;

    /**
     * Number of new reviews imported (not previously in database).
     */
    @Min(value = 0, message = "Reviews new must be non-negative")
    @Column(name = "reviews_new")
    @Builder.Default
    private Integer reviewsNew = 0;

    /**
     * Number of existing reviews updated (content changed).
     */
    @Min(value = 0, message = "Reviews updated must be non-negative")
    @Column(name = "reviews_updated")
    @Builder.Default
    private Integer reviewsUpdated = 0;

    /**
     * Error message if job failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Many-to-one relationship with ReviewSource.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_source_id", nullable = false)
    private ReviewSource reviewSource;

    /**
     * Mark job as started.
     */
    public void markAsStarted() {
        this.status = JobStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    /**
     * Mark job as completed successfully.
     */
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark job as failed with error message.
     */
    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Increment reviews fetched counter.
     */
    public void incrementReviewsFetched() {
        this.reviewsFetched = (this.reviewsFetched != null ? this.reviewsFetched : 0) + 1;
    }

    /**
     * Increment reviews new counter.
     */
    public void incrementReviewsNew() {
        this.reviewsNew = (this.reviewsNew != null ? this.reviewsNew : 0) + 1;
    }

    /**
     * Increment reviews updated counter.
     */
    public void incrementReviewsUpdated() {
        this.reviewsUpdated = (this.reviewsUpdated != null ? this.reviewsUpdated : 0) + 1;
    }

    /**
     * Calculate job duration.
     */
    public Duration getDuration() {
        if (startedAt == null) {
            return Duration.ZERO;
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, endTime);
    }

    /**
     * Check if job is still running.
     */
    public boolean isRunning() {
        return status == JobStatus.IN_PROGRESS;
    }

    /**
     * Check if job completed successfully.
     */
    public boolean isSuccessful() {
        return status == JobStatus.COMPLETED;
    }

    /**
     * Check if job failed.
     */
    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }

    /**
     * Check if job is pending.
     */
    public boolean isPending() {
        return status == JobStatus.PENDING;
    }
}
