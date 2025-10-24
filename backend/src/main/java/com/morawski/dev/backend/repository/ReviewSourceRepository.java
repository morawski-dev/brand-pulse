package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.dto.common.SourceType;
import com.morawski.dev.backend.dto.common.SyncStatus;
import com.morawski.dev.backend.entity.ReviewSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReviewSource entity.
 * Provides data access methods for review source management, sync scheduling, and plan limit enforcement.
 *
 * User Stories:
 * - US-003: Configuring First Source
 * - US-006: Switching Between Locations
 * - US-009: Free Plan Limitation
 *
 * API Endpoints:
 * - POST /api/brands/{brandId}/review-sources
 * - GET /api/brands/{brandId}/review-sources
 * - GET /api/brands/{brandId}/review-sources/{sourceId}
 * - PATCH /api/brands/{brandId}/review-sources/{sourceId}
 * - DELETE /api/brands/{brandId}/review-sources/{sourceId}
 */
@Repository
public interface ReviewSourceRepository extends JpaRepository<ReviewSource, Long> {

    /**
     * Find all review sources for a specific brand.
     * Returns only non-deleted sources.
     *
     * API: GET /api/brands/{brandId}/review-sources
     *
     * @param brandId the brand ID
     * @return list of review sources
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.brand.id = :brandId AND rs.deletedAt IS NULL")
    List<ReviewSource> findByBrandId(@Param("brandId") Long brandId);

    /**
     * Find a specific review source by ID and brand ID (for authorization).
     * Ensures source belongs to the specified brand.
     *
     * API: GET /api/brands/{brandId}/review-sources/{sourceId}
     *
     * @param id the review source ID
     * @param brandId the brand ID
     * @return Optional containing source if found and belongs to brand
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.id = :id AND rs.brand.id = :brandId " +
           "AND rs.deletedAt IS NULL")
    Optional<ReviewSource> findByIdAndBrandId(@Param("id") Long id, @Param("brandId") Long brandId);

    /**
     * Count active review sources for a brand (for plan limit enforcement).
     * Free plan allows maximum 1 source.
     *
     * API: POST /api/brands/{brandId}/review-sources (US-009 validation)
     *
     * @param brandId the brand ID
     * @return count of active sources
     */
    @Query("SELECT COUNT(rs) FROM ReviewSource rs WHERE rs.brand.id = :brandId " +
           "AND rs.deletedAt IS NULL")
    long countByBrandIdAndDeletedAtIsNull(@Param("brandId") Long brandId);

    /**
     * Check if a source with the same type and external ID already exists for the brand.
     * Prevents duplicate source configuration.
     *
     * API: POST /api/brands/{brandId}/review-sources (duplicate check)
     *
     * @param brandId the brand ID
     * @param sourceType the source type (GOOGLE, FACEBOOK, TRUSTPILOT)
     * @param externalProfileId the external profile ID
     * @return true if duplicate exists
     */
    @Query("SELECT CASE WHEN COUNT(rs) > 0 THEN true ELSE false END FROM ReviewSource rs " +
           "WHERE rs.brand.id = :brandId AND rs.sourceType = :sourceType " +
           "AND rs.externalProfileId = :externalProfileId AND rs.deletedAt IS NULL")
    boolean existsByBrandIdAndSourceTypeAndExternalProfileId(
            @Param("brandId") Long brandId,
            @Param("sourceType") SourceType sourceType,
            @Param("externalProfileId") String externalProfileId
    );

    /**
     * Find review source by brand, source type, and external profile ID.
     * Used for duplicate detection with detailed response.
     *
     * @param brandId the brand ID
     * @param sourceType the source type
     * @param externalProfileId the external profile ID
     * @return Optional containing source if found
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.brand.id = :brandId " +
           "AND rs.sourceType = :sourceType AND rs.externalProfileId = :externalProfileId " +
           "AND rs.deletedAt IS NULL")
    Optional<ReviewSource> findByBrandIdAndSourceTypeAndExternalProfileId(
            @Param("brandId") Long brandId,
            @Param("sourceType") SourceType sourceType,
            @Param("externalProfileId") String externalProfileId
    );

    /**
     * Find all active review sources (is_active = true).
     * Used for sync job scheduling.
     *
     * @return list of active sources
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.isActive = true AND rs.deletedAt IS NULL")
    List<ReviewSource> findByIsActiveTrue();

    /**
     * Find review sources that need to be synced (scheduled sync time has passed).
     * Used by CRON job to determine which sources to sync.
     *
     * Daily CRON: 3:00 AM CET
     *
     * @param now current timestamp
     * @return list of sources ready for sync
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.isActive = true " +
           "AND rs.nextScheduledSyncAt <= :now AND rs.deletedAt IS NULL")
    List<ReviewSource> findByNextScheduledSyncAtBeforeAndIsActiveTrue(@Param("now") Instant now);

    /**
     * Find review sources by last sync status.
     * Used for monitoring and error reporting.
     *
     * @param status the sync status (SUCCESS, FAILED, IN_PROGRESS)
     * @return list of sources with specified status
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.lastSyncStatus = :status " +
           "AND rs.deletedAt IS NULL")
    List<ReviewSource> findByLastSyncStatus(@Param("status") SyncStatus status);

    /**
     * Find review sources that failed their last sync.
     * Used for retry logic and alerting.
     *
     * @return list of sources with failed sync
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.lastSyncStatus = 'FAILED' " +
           "AND rs.isActive = true AND rs.deletedAt IS NULL")
    List<ReviewSource> findFailedSources();

    /**
     * Find all review sources for a specific user (across all brands).
     * Used for user-level analytics.
     *
     * @param userId the user ID
     * @return list of user's review sources
     */
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.brand.user.id = :userId " +
           "AND rs.deletedAt IS NULL")
    List<ReviewSource> findByUserId(@Param("userId") Long userId);

    /**
     * Count review sources by type (for analytics).
     *
     * @param sourceType the source type
     * @return count of sources with specified type
     */
    @Query("SELECT COUNT(rs) FROM ReviewSource rs WHERE rs.sourceType = :sourceType " +
           "AND rs.deletedAt IS NULL")
    long countBySourceType(@Param("sourceType") SourceType sourceType);
}
