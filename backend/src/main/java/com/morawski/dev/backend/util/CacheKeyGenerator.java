package com.morawski.dev.backend.util;

import java.util.stream.Collectors;
import java.util.List;

/**
 * Utility class for generating cache keys.
 * Ensures consistent cache key formatting across the application.
 */
public final class CacheKeyGenerator {

    private CacheKeyGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates cache key for dashboard summary.
     * Format: "dashboard:brand:{brandId}:source:{sourceId}"
     *
     * @param brandId the brand ID
     * @param sourceId the source ID (null for "all sources")
     * @return cache key
     */
    public static String dashboardKey(Long brandId, Long sourceId) {
        if (sourceId == null) {
            return Constants.CACHE_PREFIX_DASHBOARD + brandId;
        }
        return Constants.CACHE_PREFIX_DASHBOARD + brandId + ":source:" + sourceId;
    }

    /**
     * Generates cache key for AI summary.
     * Format: "summary:source:{sourceId}"
     *
     * @param sourceId the source ID
     * @return cache key
     */
    public static String summaryKey(Long sourceId) {
        return Constants.CACHE_PREFIX_SUMMARY + sourceId;
    }

    /**
     * Generates cache key for review list with filters.
     * Format: "reviews:brand:{brandId}:filters:{filterHash}"
     *
     * @param brandId the brand ID
     * @param sourceId optional source ID filter
     * @param sentiment optional sentiment filter
     * @param rating optional rating filter
     * @return cache key
     */
    public static String reviewsKey(Long brandId, Long sourceId, String sentiment, Integer rating) {
        StringBuilder key = new StringBuilder(Constants.CACHE_PREFIX_REVIEWS)
            .append(brandId)
            .append(":filters:");

        StringBuilder filters = new StringBuilder();
        if (sourceId != null) {
            filters.append("source:").append(sourceId).append("|");
        }
        if (sentiment != null) {
            filters.append("sentiment:").append(sentiment).append("|");
        }
        if (rating != null) {
            filters.append("rating:").append(rating).append("|");
        }

        // Generate hash of filters for shorter key
        String filterHash = HashUtils.sha256(filters.toString()).substring(0, 16);
        key.append(filterHash);

        return key.toString();
    }

    /**
     * Generates cache key for review list with multiple filter values.
     * Format: "reviews:brand:{brandId}:filters:{filterHash}"
     *
     * @param brandId the brand ID
     * @param sourceId optional source ID filter
     * @param sentiments optional list of sentiment filters
     * @param ratings optional list of rating filters
     * @return cache key
     */
    public static String reviewsKeyMulti(Long brandId, Long sourceId, List<String> sentiments, List<Integer> ratings) {
        StringBuilder key = new StringBuilder(Constants.CACHE_PREFIX_REVIEWS)
            .append(brandId)
            .append(":filters:");

        StringBuilder filters = new StringBuilder();
        if (sourceId != null) {
            filters.append("source:").append(sourceId).append("|");
        }
        if (sentiments != null && !sentiments.isEmpty()) {
            String sentimentStr = sentiments.stream()
                .sorted()
                .collect(Collectors.joining(","));
            filters.append("sentiment:").append(sentimentStr).append("|");
        }
        if (ratings != null && !ratings.isEmpty()) {
            String ratingStr = ratings.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
            filters.append("rating:").append(ratingStr).append("|");
        }

        // Generate hash of filters for shorter key
        String filterHash = HashUtils.sha256(filters.toString()).substring(0, 16);
        key.append(filterHash);

        return key.toString();
    }

    /**
     * Generates wildcard pattern for cache eviction.
     * Example: "reviews:brand:1:*" to clear all review caches for brand 1
     *
     * @param prefix the cache prefix
     * @param id the ID to match
     * @return wildcard pattern
     */
    public static String wildcardPattern(String prefix, Long id) {
        return prefix + id + ":*";
    }

    /**
     * Generates cache key for user session data.
     * Format: "session:user:{userId}"
     *
     * @param userId the user ID
     * @return cache key
     */
    public static String userSessionKey(Long userId) {
        return "session:user:" + userId;
    }

    /**
     * Generates cache key for rate limit tracking.
     * Format: "ratelimit:user:{userId}:{endpoint}"
     *
     * @param userId the user ID
     * @param endpoint the endpoint path
     * @return cache key
     */
    public static String rateLimitKey(Long userId, String endpoint) {
        String sanitizedEndpoint = endpoint.replaceAll("[^a-zA-Z0-9]", "_");
        return "ratelimit:user:" + userId + ":" + sanitizedEndpoint;
    }

    /**
     * Generates cache key for IP-based rate limiting.
     * Format: "ratelimit:ip:{ipAddress}:{endpoint}"
     *
     * @param ipAddress the client IP address
     * @param endpoint the endpoint path
     * @return cache key
     */
    public static String rateLimitIpKey(String ipAddress, String endpoint) {
        String sanitizedIp = ipAddress.replaceAll("[^0-9.]", "_");
        String sanitizedEndpoint = endpoint.replaceAll("[^a-zA-Z0-9]", "_");
        return "ratelimit:ip:" + sanitizedIp + ":" + sanitizedEndpoint;
    }

    /**
     * Generates cache key for JWT token blacklist.
     * Format: "blacklist:token:{tokenHash}"
     *
     * @param token the JWT token
     * @return cache key
     */
    public static String tokenBlacklistKey(String token) {
        String tokenHash = HashUtils.sha256(token).substring(0, 16);
        return "blacklist:token:" + tokenHash;
    }

    /**
     * Generates cache key for aggregated metrics.
     * Format: "metrics:brand:{brandId}:date:{date}"
     *
     * @param brandId the brand ID
     * @param date the date string (yyyy-MM-dd)
     * @return cache key
     */
    public static String metricsKey(Long brandId, String date) {
        return "metrics:brand:" + brandId + ":date:" + date;
    }

    /**
     * Generates cache key for sync job status.
     * Format: "sync:job:{jobId}"
     *
     * @param jobId the sync job ID
     * @return cache key
     */
    public static String syncJobKey(Long jobId) {
        return "sync:job:" + jobId;
    }
}
