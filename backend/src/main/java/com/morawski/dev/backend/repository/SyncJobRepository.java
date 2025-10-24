package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.dto.common.JobStatus;
import com.morawski.dev.backend.dto.common.JobType;
import com.morawski.dev.backend.entity.SyncJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SyncJob entity.
 * Provides data access methods for sync job tracking and monitoring.
 *
 * User Stories:
 * - US-003: Configuring First Source (initial 90-day import)
 * - US-008: Manual Data Refresh
 *
 * API Endpoints:
 * - POST /api/brands/{brandId}/sync
 * - GET /api/sync-jobs/{jobId}
 * - GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs
 */
@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

    /**
     * Find all sync jobs for a specific review source.
     * Returns jobs ordered by creation date descending.
     *
     * API: GET /api/brands/{brandId}/review-sources/{sourceId}/sync-jobs
     *
     * @param reviewSourceId the review source ID
     * @param pageable pagination parameters
     * @return page of sync jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "ORDER BY sj.createdAt DESC")
    Page<SyncJob> findByReviewSourceId(
            @Param("reviewSourceId") Long reviewSourceId,
            Pageable pageable
    );

    /**
     * Find sync jobs by status.
     * Used for monitoring and alerting.
     *
     * @param status the job status
     * @return list of jobs with specified status
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.status = :status ORDER BY sj.createdAt DESC")
    List<SyncJob> findByStatus(@Param("status") JobStatus status);

    /**
     * Find sync jobs by type and status.
     * Used for operational monitoring.
     *
     * @param jobType the job type
     * @param status the job status
     * @return list of matching jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.jobType = :jobType AND sj.status = :status " +
           "ORDER BY sj.createdAt DESC")
    List<SyncJob> findByJobTypeAndStatus(
            @Param("jobType") JobType jobType,
            @Param("status") JobStatus status
    );

    /**
     * Find all pending jobs (not yet started).
     * Used by job processor to pick up work.
     *
     * @return list of pending jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.status = 'PENDING' ORDER BY sj.createdAt ASC")
    List<SyncJob> findPendingJobs();

    /**
     * Find all running jobs (in progress).
     * Used for monitoring and detecting stuck jobs.
     *
     * @return list of running jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.status = 'IN_PROGRESS' ORDER BY sj.startedAt ASC")
    List<SyncJob> findRunningJobs();

    /**
     * Find stuck jobs (in progress for more than specified duration).
     * Used for detecting and handling hung jobs.
     *
     * @param threshold timestamp threshold (jobs started before this are considered stuck)
     * @return list of stuck jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.status = 'IN_PROGRESS' " +
           "AND sj.startedAt < :threshold ORDER BY sj.startedAt ASC")
    List<SyncJob> findStuckJobs(@Param("threshold") Instant threshold);

    /**
     * Check if there is an active job (pending or in progress) for a review source.
     * Used to prevent duplicate scheduling or manual triggers while job is running.
     *
     * @param reviewSourceId the review source ID
     * @return true if an active job exists
     */
    @Query("SELECT CASE WHEN COUNT(sj) > 0 THEN true ELSE false END FROM SyncJob sj " +
           "WHERE sj.reviewSource.id = :reviewSourceId AND sj.status IN ('PENDING', 'IN_PROGRESS')")
    boolean existsActiveJobForSource(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Find most recent sync job for a review source.
     * Used to display last sync status.
     *
     * @param reviewSourceId the review source ID
     * @return Optional containing most recent job
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "ORDER BY sj.createdAt DESC LIMIT 1")
    Optional<SyncJob> findMostRecentByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Find most recent completed job for a review source.
     * Used to determine when last successful sync occurred.
     *
     * @param reviewSourceId the review source ID
     * @return Optional containing most recent completed job
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "AND sj.status = 'COMPLETED' ORDER BY sj.completedAt DESC LIMIT 1")
    Optional<SyncJob> findMostRecentCompletedByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Count jobs by status for a review source.
     * Used for metrics and monitoring.
     *
     * @param reviewSourceId the review source ID
     * @param status the job status
     * @return count of jobs
     */
    @Query("SELECT COUNT(sj) FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "AND sj.status = :status")
    long countByReviewSourceIdAndStatus(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("status") JobStatus status
    );

    /**
     * Find failed jobs within date range.
     * Used for error analysis and reporting.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of failed jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.status = 'FAILED' " +
           "AND sj.createdAt >= :startDate AND sj.createdAt <= :endDate " +
           "ORDER BY sj.createdAt DESC")
    List<SyncJob> findFailedJobsBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find jobs by type for a review source.
     * Used for filtering job history.
     *
     * @param reviewSourceId the review source ID
     * @param jobType the job type
     * @param pageable pagination parameters
     * @return page of jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "AND sj.jobType = :jobType ORDER BY sj.createdAt DESC")
    Page<SyncJob> findByReviewSourceIdAndJobType(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("jobType") JobType jobType,
            Pageable pageable
    );

    /**
     * Find jobs by status for a review source with pagination.
     *
     * @param reviewSourceId the review source ID
     * @param status the job status
     * @param pageable pagination parameters
     * @return page of jobs
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "AND sj.status = :status ORDER BY sj.createdAt DESC")
    Page<SyncJob> findByReviewSourceIdAndStatus(
            @Param("reviewSourceId") Long reviewSourceId,
            @Param("status") JobStatus status,
            Pageable pageable
    );

    /**
     * Calculate average job duration for a review source.
     * Used for performance metrics.
     *
     * @param reviewSourceId the review source ID
     * @return average duration in seconds
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM sj.completedAt) - EXTRACT(EPOCH FROM sj.startedAt)) " +
           "FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "AND sj.status = 'COMPLETED' AND sj.startedAt IS NOT NULL AND sj.completedAt IS NOT NULL")
    Double calculateAverageDuration(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Find initial import job for a review source.
     * Used to track onboarding progress (US-003).
     *
     * @param reviewSourceId the review source ID
     * @return Optional containing initial import job
     */
    @Query("SELECT sj FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId " +
           "AND sj.jobType = 'INITIAL' ORDER BY sj.createdAt ASC LIMIT 1")
    Optional<SyncJob> findInitialImportJob(@Param("reviewSourceId") Long reviewSourceId);

    /**
     * Count total jobs for a review source.
     *
     * @param reviewSourceId the review source ID
     * @return total job count
     */
    @Query("SELECT COUNT(sj) FROM SyncJob sj WHERE sj.reviewSource.id = :reviewSourceId")
    long countByReviewSourceId(@Param("reviewSourceId") Long reviewSourceId);
}
