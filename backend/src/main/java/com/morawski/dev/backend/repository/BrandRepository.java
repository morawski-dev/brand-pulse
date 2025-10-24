package com.morawski.dev.backend.repository;

import com.morawski.dev.backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Brand entity.
 * Provides data access methods for brand management and manual refresh rate limiting.
 *
 * User Stories:
 * - US-003: Configuring First Source
 * - US-008: Manual Data Refresh
 *
 * API Endpoints:
 * - POST /api/brands
 * - GET /api/brands
 * - GET /api/brands/{brandId}
 * - PATCH /api/brands/{brandId}
 * - DELETE /api/brands/{brandId}
 * - POST /api/brands/{brandId}/sync
 *
 * MVP Constraint: One brand per user.
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Find all brands belonging to a specific user.
     * Returns only non-deleted brands.
     *
     * API: GET /api/brands
     *
     * @param userId the user ID
     * @return list of user's brands
     */
    @Query("SELECT b FROM Brand b WHERE b.user.id = :userId AND b.deletedAt IS NULL")
    List<Brand> findByUserId(@Param("userId") Long userId);

    /**
     * Find a specific brand by ID and user ID (for authorization check).
     * Ensures user owns the brand before allowing access.
     *
     * API: GET /api/brands/{brandId}
     *
     * @param id the brand ID
     * @param userId the user ID
     * @return Optional containing brand if found and owned by user
     */
    @Query("SELECT b FROM Brand b WHERE b.id = :id AND b.user.id = :userId AND b.deletedAt IS NULL")
    Optional<Brand> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Count how many brands a user has.
     * Used to enforce MVP constraint: one brand per user.
     *
     * API: POST /api/brands (validation)
     *
     * @param userId the user ID
     * @return count of user's brands
     */
    @Query("SELECT COUNT(b) FROM Brand b WHERE b.user.id = :userId AND b.deletedAt IS NULL")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Check if user already has a brand (MVP: only one allowed).
     *
     * @param userId the user ID
     * @return true if user has a brand
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Brand b " +
           "WHERE b.user.id = :userId AND b.deletedAt IS NULL")
    boolean existsByUserId(@Param("userId") Long userId);

    /**
     * Find brands that can perform manual refresh (24h cooldown passed).
     * Used for manual refresh rate limiting check.
     *
     * API: POST /api/brands/{brandId}/sync (validation)
     *
     * @param brandId the brand ID
     * @param twentyFourHoursAgo timestamp 24 hours ago
     * @return Optional containing brand if manual refresh is allowed
     */
    @Query("SELECT b FROM Brand b WHERE b.id = :brandId AND b.deletedAt IS NULL " +
           "AND (b.lastManualRefreshAt IS NULL OR b.lastManualRefreshAt < :twentyFourHoursAgo)")
    Optional<Brand> findByIdAndCanManualRefresh(
            @Param("brandId") Long brandId,
            @Param("twentyFourHoursAgo") Instant twentyFourHoursAgo
    );

    /**
     * Find brand with review sources eagerly loaded.
     * Used for dashboard and source listing.
     *
     * @param id the brand ID
     * @return Optional containing brand with sources
     */
    @Query("SELECT b FROM Brand b LEFT JOIN FETCH b.reviewSources " +
           "WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Brand> findByIdWithSources(@Param("id") Long id);

    /**
     * Find brand by ID and user ID with review sources eagerly loaded.
     * Combines ownership check with eager loading.
     *
     * API: GET /api/brands/{brandId} (with sources)
     *
     * @param id the brand ID
     * @param userId the user ID
     * @return Optional containing brand with sources if owned by user
     */
    @Query("SELECT b FROM Brand b LEFT JOIN FETCH b.reviewSources rs " +
           "WHERE b.id = :id AND b.user.id = :userId AND b.deletedAt IS NULL " +
           "AND (rs.deletedAt IS NULL OR rs.deletedAt IS NOT NULL)")
    Optional<Brand> findByIdAndUserIdWithSources(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    /**
     * Count all active brands (for analytics).
     *
     * @return count of active brands
     */
    @Query("SELECT COUNT(b) FROM Brand b WHERE b.deletedAt IS NULL")
    long countActiveBrands();
}
