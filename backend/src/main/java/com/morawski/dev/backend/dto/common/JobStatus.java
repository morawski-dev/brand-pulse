package com.morawski.dev.backend.dto.common;

/**
 * Status of a synchronization job.
 * Maps to sync_jobs.status in database.
 */
public enum JobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
