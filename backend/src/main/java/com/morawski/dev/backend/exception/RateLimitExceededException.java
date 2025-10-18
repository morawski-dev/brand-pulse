package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * Exception thrown when user exceeds rate limits.
 * Results in HTTP 429 Too Many Requests response.
 *
 * Used when:
 * - Manual refresh triggered within 24 hours (US-008)
 * - Login attempts exceed threshold (brute-force protection)
 * - API request rate limits exceeded
 *
 * Example usage:
 * <pre>
 * Instant lastRefresh = brand.getLastManualRefreshAt();
 * if (lastRefresh != null && lastRefresh.isAfter(Instant.now().minus(Duration.ofHours(24)))) {
 *     Instant nextAvailable = lastRefresh.plus(Duration.ofHours(24));
 *     long hoursRemaining = Duration.between(Instant.now(), nextAvailable).toHours();
 *     throw new RateLimitExceededException(
 *         "Manual sync can only be triggered once per 24 hours",
 *         lastRefresh,
 *         nextAvailable,
 *         hoursRemaining
 *     );
 * }
 * </pre>
 */
public class RateLimitExceededException extends BrandPulseException {

    private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    /**
     * Constructor for manual refresh rate limit (24h cooldown).
     *
     * @param message Error message
     * @param lastRefreshAt When the last refresh occurred
     * @param nextAvailableAt When the next refresh is allowed
     * @param hoursRemaining Hours until next refresh is allowed
     */
    public RateLimitExceededException(
        String message,
        Instant lastRefreshAt,
        Instant nextAvailableAt,
        long hoursRemaining
    ) {
        super(ERROR_CODE, message, HttpStatus.TOO_MANY_REQUESTS);
        addDetail("lastManualRefreshAt", lastRefreshAt);
        addDetail("nextAvailableAt", nextAvailableAt);
        addDetail("hoursRemaining", hoursRemaining);
    }

    /**
     * Constructor for generic rate limit with retry information.
     *
     * @param message Error message
     * @param retryAfterSeconds Seconds to wait before retrying
     */
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(ERROR_CODE, message, HttpStatus.TOO_MANY_REQUESTS);
        addDetail("retryAfter", retryAfterSeconds);
        addDetail("retryAt", Instant.now().plusSeconds(retryAfterSeconds));
    }

    /**
     * Constructor with custom message.
     *
     * @param message Error message
     */
    public RateLimitExceededException(String message) {
        super(ERROR_CODE, message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
