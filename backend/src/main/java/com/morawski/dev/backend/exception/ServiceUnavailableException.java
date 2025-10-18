package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Exception thrown when the application is temporarily unavailable.
 * Results in HTTP 503 Service Unavailable response.
 *
 * Used when:
 * - Database connection is down
 * - Application is in maintenance mode
 * - System resources are exhausted
 * - Temporary service degradation
 *
 * Example usage:
 * <pre>
 * if (maintenanceMode) {
 *     throw new ServiceUnavailableException(
 *         "System is under maintenance",
 *         estimatedRecoveryTime
 *     );
 * }
 * </pre>
 */
public class ServiceUnavailableException extends BrandPulseException {

    private static final String ERROR_CODE = "SERVICE_UNAVAILABLE";

    /**
     * Constructor with message and estimated recovery time.
     *
     * @param message Error message
     * @param retryAfter When the service is expected to be available (seconds)
     */
    public ServiceUnavailableException(String message, long retryAfter) {
        super(ERROR_CODE, message, HttpStatus.SERVICE_UNAVAILABLE);
        addDetail("retryAfter", retryAfter);
        addDetail("estimatedRecoveryAt", Instant.now().plusSeconds(retryAfter));
    }

    /**
     * Constructor with message only.
     *
     * @param message Error message
     */
    public ServiceUnavailableException(String message) {
        super(ERROR_CODE, message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Default constructor for generic maintenance mode.
     */
    public ServiceUnavailableException() {
        super(
            ERROR_CODE,
            "Service is temporarily unavailable. Please try again later.",
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}
