package com.morawski.dev.backend.dto.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result object returned by sync handlers after processing a sync job.
 * Contains statistics about the sync operation.
 *
 * Used by:
 * - GoogleReviewSyncHandler
 * - FacebookReviewSyncHandler (Phase 2)
 * - TrustpilotReviewSyncHandler (Phase 2)
 *
 * API Plan Section 15.3: Background Jobs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /**
     * Total number of reviews fetched from external source.
     */
    private int reviewsFetched;

    /**
     * Number of new reviews added to database.
     */
    private int reviewsNew;

    /**
     * Number of existing reviews updated.
     */
    private int reviewsUpdated;

    /**
     * Whether sync completed successfully.
     */
    private boolean success;

    /**
     * Error message if sync failed.
     */
    private String errorMessage;

    /**
     * Create successful sync result.
     *
     * @param fetched Total reviews fetched
     * @param newCount New reviews created
     * @param updated Reviews updated
     * @return SyncResult
     */
    public static SyncResult success(int fetched, int newCount, int updated) {
        return SyncResult.builder()
            .reviewsFetched(fetched)
            .reviewsNew(newCount)
            .reviewsUpdated(updated)
            .success(true)
            .build();
    }

    /**
     * Create failed sync result.
     *
     * @param errorMessage Error description
     * @return SyncResult
     */
    public static SyncResult failure(String errorMessage) {
        return SyncResult.builder()
            .reviewsFetched(0)
            .reviewsNew(0)
            .reviewsUpdated(0)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
