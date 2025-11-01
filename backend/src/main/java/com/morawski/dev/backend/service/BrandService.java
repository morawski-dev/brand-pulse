package com.morawski.dev.backend.service;

import com.morawski.dev.backend.dto.brand.*;
import com.morawski.dev.backend.dto.source.ReviewSourceSummary;
import com.morawski.dev.backend.entity.Brand;
import com.morawski.dev.backend.entity.ReviewSource;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.BrandLimitExceededException;
import com.morawski.dev.backend.exception.ResourceAccessDeniedException;
import com.morawski.dev.backend.exception.ResourceNotFoundException;
import com.morawski.dev.backend.exception.ValidationException;
import com.morawski.dev.backend.mapper.BrandMapper;
import com.morawski.dev.backend.mapper.ReviewSourceMapper;
import com.morawski.dev.backend.repository.BrandRepository;
import com.morawski.dev.backend.util.Constants;
import com.morawski.dev.backend.util.StringUtils;
import com.morawski.dev.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for brand management operations.
 * Handles CRUD operations for brands with ownership validation.
 *
 * API Endpoints (Section 5):
 * - POST /api/brands (Section 5.1)
 * - GET /api/brands (Section 5.2)
 * - GET /api/brands/{brandId} (Section 5.3)
 * - PATCH /api/brands/{brandId} (Section 5.4)
 * - DELETE /api/brands/{brandId} (Section 5.5)
 *
 * User Stories:
 * - US-003: Configuring First Source (Step 1: Create Brand)
 *
 * MVP Constraint:
 * - One brand per user account
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserService userService;
    private final BrandMapper brandMapper;
    private final ReviewSourceMapper reviewSourceMapper;

    /**
     * Create new brand for user.
     * API: POST /api/brands (Section 5.1)
     *
     * Business Logic:
     * - MVP: Only one brand per user
     * - Brand name: 1-255 characters
     * - Returns 409 Conflict if user already has a brand
     *
     * @param userId User ID from JWT token
     * @param request Create brand request containing name
     * @return BrandResponse with created brand details
     * @throws BrandLimitExceededException if user already has a brand (MVP limitation)
     * @throws ValidationException if brand name is invalid
     */
    @Transactional
    public BrandResponse createBrand(Long userId, CreateBrandRequest request) {
        log.info("Creating brand for user ID: {}", userId);

        // Verify user exists
        User user = userService.findByIdOrThrow(userId);

        // MVP constraint: Check if user already has a brand
        if (brandRepository.existsByUserId(userId)) {
            log.warn("Brand creation failed: User {} already has a brand (MVP limitation)", userId);
            throw new BrandLimitExceededException();
        }

        // Validate brand name
        if (!ValidationUtils.isValidBrandName(request.getName(), Constants.MAX_BRAND_NAME_LENGTH)) {
            throw new ValidationException("Invalid brand name");
        }

        // Create brand entity
        Brand brand = Brand.builder()
            .name(request.getName().trim())
            .user(user)
            .build();

        Brand savedBrand = brandRepository.save(brand);

        log.info("Brand created successfully: {} (ID: {}) for user: {}",
            savedBrand.getName(), savedBrand.getId(), userId);

        return brandMapper.toBrandResponse(savedBrand);
    }

    /**
     * Get all brands for authenticated user.
     * API: GET /api/brands (Section 5.2)
     *
     * Business Logic:
     * - Filter brands by authenticated user (JWT userId)
     * - Exclude soft-deleted brands (deleted_at IS NULL)
     * - Include count of active review sources
     *
     * @param userId User ID from JWT token
     * @return BrandListResponse containing list of user's brands
     */
    @Transactional(readOnly = true)
    public BrandListResponse getUserBrands(Long userId) {
        log.debug("Getting brands for user ID: {}", userId);

        List<Brand> brands = brandRepository.findByUserId(userId);

        List<BrandResponse> brandResponses = brands.stream()
            .map(brandMapper::toBrandResponse)
            .collect(Collectors.toList());

        log.info("Retrieved {} brand(s) for user ID: {}", brandResponses.size(), userId);

        return BrandListResponse.builder()
            .brands(brandResponses)
            .build();
    }

    /**
     * Get brand by ID with ownership validation.
     * API: GET /api/brands/{brandId} (Section 5.3)
     *
     * Business Logic:
     * - Verify user owns the brand
     * - Include list of review sources
     * - Return 403 Forbidden if user doesn't own brand
     * - Return 404 Not Found if brand doesn't exist
     *
     * @param brandId Brand ID
     * @param userId User ID from JWT token
     * @return BrandDetailResponse with brand and sources
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public BrandDetailResponse getBrandById(Long brandId, Long userId) {
        log.debug("Getting brand ID: {} for user ID: {}", brandId, userId);

        Brand brand = findByIdAndUserIdOrThrow(brandId, userId);

        // Map review sources to summary DTOs
        List<ReviewSourceSummary> sourceSummaries = brand.getReviewSources().stream()
            .filter(source -> source.getDeletedAt() == null) // Only active sources
            .map(reviewSourceMapper::toReviewSourceSummary)
            .collect(Collectors.toList());

        // Build detailed response
        BrandDetailResponse response = BrandDetailResponse.builder()
            .brandId(brand.getId())
            .name(brand.getName())
            .sourceCount(sourceSummaries.size())
            .lastManualRefreshAt(brand.getLastManualRefreshAt() != null
                ? brand.getLastManualRefreshAt().atZone(java.time.ZoneId.of("UTC"))
                : null)
            .sources(sourceSummaries)
            .createdAt(brand.getCreatedAt().atZone(java.time.ZoneId.of("UTC")))
            .updatedAt(brand.getUpdatedAt().atZone(java.time.ZoneId.of("UTC")))
            .build();

        log.info("Retrieved brand: {} (ID: {}) with {} sources",
            brand.getName(), brandId, sourceSummaries.size());

        return response;
    }

    /**
     * Update brand name.
     * API: PATCH /api/brands/{brandId} (Section 5.4)
     *
     * Business Logic:
     * - Only brand name can be updated
     * - Verify user owns the brand
     *
     * @param brandId Brand ID
     * @param userId User ID from JWT token
     * @param request Update request containing new name
     * @return Updated BrandResponse
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     * @throws ValidationException if brand name is invalid
     */
    @Transactional
    public BrandResponse updateBrand(Long brandId, Long userId, UpdateBrandRequest request) {
        log.info("Updating brand ID: {} for user ID: {}", brandId, userId);

        Brand brand = findByIdAndUserIdOrThrow(brandId, userId);

        // Update brand name if provided
        if (request.getName() != null) {
            if (!ValidationUtils.isValidBrandName(request.getName(), Constants.MAX_BRAND_NAME_LENGTH)) {
                throw new ValidationException("Invalid brand name");
            }

            String oldName = brand.getName();
            brand.setName(request.getName().trim());

            log.info("Brand name updated: {} -> {} (ID: {})", oldName, brand.getName(), brandId);
        }

        Brand savedBrand = brandRepository.save(brand);

        return brandMapper.toBrandResponse(savedBrand);
    }

    /**
     * Soft delete brand.
     * API: DELETE /api/brands/{brandId} (Section 5.5)
     *
     * Business Logic:
     * - Soft delete: Set deleted_at=NOW()
     * - Cascade: All review_sources, reviews, dashboard_aggregates are also soft-deleted (ON DELETE CASCADE)
     * - Hard delete after 90 days (background job - future implementation)
     *
     * @param brandId Brand ID
     * @param userId User ID from JWT token
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional
    public void deleteBrand(Long brandId, Long userId) {
        log.info("Deleting brand ID: {} for user ID: {}", brandId, userId);

        Brand brand = findByIdAndUserIdOrThrow(brandId, userId);

        // Soft delete brand
        brand.softDelete();

        // Cascade soft delete to review sources
        for (ReviewSource source : brand.getReviewSources()) {
            if (source.getDeletedAt() == null) {
                source.softDelete();
            }
        }

        brandRepository.save(brand);

        log.warn("Brand soft deleted: {} (ID: {})", brand.getName(), brandId);
    }

    // ========== Helper Methods ==========

    /**
     * Find brand by ID.
     *
     * @param brandId Brand ID
     * @return Brand entity
     * @throws ResourceNotFoundException if brand not found
     */
    @Transactional(readOnly = true)
    public Brand findByIdOrThrow(Long brandId) {
        return brandRepository.findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", brandId));
    }

    /**
     * Find brand by ID with ownership validation.
     * Ensures user owns the brand before allowing access.
     *
     * @param brandId Brand ID
     * @param userId User ID
     * @return Brand entity
     * @throws ResourceNotFoundException if brand not found
     * @throws ResourceAccessDeniedException if user doesn't own brand
     */
    @Transactional(readOnly = true)
    public Brand findByIdAndUserIdOrThrow(Long brandId, Long userId) {
        // First check if brand exists at all
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", brandId));

        // Then check ownership
        if (!brand.getUser().getId().equals(userId)) {
            log.warn("Access denied: User {} attempted to access brand {} owned by user {}",
                userId, brandId, brand.getUser().getId());
            throw new ResourceAccessDeniedException(
                "You do not have permission to access this brand"
            );
        }

        return brand;
    }

    /**
     * Find brand by ID and user ID with sources eagerly loaded.
     *
     * @param brandId Brand ID
     * @param userId User ID
     * @return Brand entity with sources
     * @throws ResourceNotFoundException if brand not found or not owned by user
     */
    @Transactional(readOnly = true)
    public Brand findByIdAndUserIdWithSourcesOrThrow(Long brandId, Long userId) {
        return brandRepository.findByIdAndUserIdWithSources(brandId, userId)
            .orElseThrow(() -> {
                // First check if brand exists to provide better error message
                if (!brandRepository.existsById(brandId)) {
                    return new ResourceNotFoundException("Brand", "id", brandId);
                }
                // Brand exists but user doesn't own it
                return new ResourceAccessDeniedException(
                    "You do not have permission to access this brand"
                );
            });
    }

    /**
     * Check if user owns a specific brand.
     *
     * @param brandId Brand ID
     * @param userId User ID
     * @return true if user owns the brand
     */
    @Transactional(readOnly = true)
    public boolean userOwnsBrand(Long brandId, Long userId) {
        return brandRepository.findByIdAndUserId(brandId, userId).isPresent();
    }

    /**
     * Count brands for a user.
     *
     * @param userId User ID
     * @return Number of brands
     */
    @Transactional(readOnly = true)
    public long countUserBrands(Long userId) {
        return brandRepository.countByUserId(userId);
    }

    /**
     * Check if user has any brands.
     *
     * @param userId User ID
     * @return true if user has at least one brand
     */
    @Transactional(readOnly = true)
    public boolean userHasBrand(Long userId) {
        return brandRepository.existsByUserId(userId);
    }
}
