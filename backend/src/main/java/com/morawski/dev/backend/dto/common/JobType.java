package com.morawski.dev.backend.dto.common;

/**
 * Type of synchronization job.
 * Maps to sync_jobs.job_type in database.
 */
public enum JobType {
    SCHEDULED,
    MANUAL,
    INITIAL
}
