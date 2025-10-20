package com.morawski.dev.backend.dto.common;

/**
 * Synchronization status for review sources.
 * Maps to review_sources.last_sync_status in database.
 */
public enum SyncStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS
}
