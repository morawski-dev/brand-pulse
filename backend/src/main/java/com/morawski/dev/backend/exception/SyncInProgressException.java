package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to start a sync while another is running.
 * Results in HTTP 422 Unprocessable Entity response.
 *
 * Used when:
 * - User tries to trigger manual sync while another sync job is IN_PROGRESS
 * - Scheduled sync attempts to start while previous job is still running
 *
 * Example usage:
 * <pre>
 * boolean syncInProgress = syncJobRepository.existsByReviewSourceIdAndStatusIn(
 *     sourceId,
 *     List.of(SyncStatus.PENDING, SyncStatus.IN_PROGRESS)
 * );
 * if (syncInProgress) {
 *     throw new SyncInProgressException(sourceId);
 * }
 * </pre>
 */
public class SyncInProgressException extends BrandPulseException {

    private static final String ERROR_CODE = "SYNC_IN_PROGRESS";
    private static final String DEFAULT_MESSAGE = "A synchronization job is already in progress for this source";

    /**
     * Constructor with source ID.
     *
     * @param reviewSourceId Review source ID
     */
    public SyncInProgressException(Long reviewSourceId) {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.UNPROCESSABLE_ENTITY);
        addDetail("reviewSourceId", reviewSourceId);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Error message
     */
    public SyncInProgressException(String message) {
        super(ERROR_CODE, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
