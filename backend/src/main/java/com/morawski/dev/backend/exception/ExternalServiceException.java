package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when external service calls fail.
 * Results in HTTP 502 Bad Gateway response.
 *
 * Used when:
 * - Google My Business API returns error
 * - OpenRouter.ai (AI service) is unavailable
 * - Facebook/Trustpilot API fails
 * - Web scraper encounters errors
 *
 * Example usage:
 * <pre>
 * try {
 *     googleApiClient.fetchReviews(profileId);
 * } catch (ApiException e) {
 *     throw new ExternalServiceException("Google My Business API", e.getMessage(), e);
 * }
 * </pre>
 */
public class ExternalServiceException extends BrandPulseException {

    /**
     * Factory method for Google API errors.
     *
     * @param message Error message from Google API
     * @param cause Original exception
     * @return ExternalServiceException instance
     */
    public static ExternalServiceException forGoogleApi(String message, Throwable cause) {
        ExternalServiceException ex = new ExternalServiceException(
            "GOOGLE_API_ERROR",
            "Google My Business API error: " + message,
            cause
        );
        ex.addDetail("service", "Google My Business API");
        return ex;
    }

    /**
     * Factory method for AI service errors.
     *
     * @param message Error message from AI service
     * @param cause Original exception
     * @return ExternalServiceException instance
     */
    public static ExternalServiceException forAiService(String message, Throwable cause) {
        ExternalServiceException ex = new ExternalServiceException(
            "AI_SERVICE_UNAVAILABLE",
            "AI sentiment analysis service unavailable: " + message,
            cause
        );
        ex.addDetail("service", "OpenRouter.ai");
        return ex;
    }

    /**
     * Factory method for web scraper errors.
     *
     * @param sourceType Source type (FACEBOOK, TRUSTPILOT)
     * @param message Error message
     * @param cause Original exception
     * @return ExternalServiceException instance
     */
    public static ExternalServiceException forScraper(String sourceType, String message, Throwable cause) {
        ExternalServiceException ex = new ExternalServiceException(
            "SCRAPER_FAILED",
            String.format("%s web scraper failed: %s", sourceType, message),
            cause
        );
        ex.addDetail("service", sourceType + " Web Scraper");
        ex.addDetail("sourceType", sourceType);
        return ex;
    }

    /**
     * Private constructor with error code and message (no cause).
     *
     * @param errorCode Specific error code
     * @param message Error message
     */
    private ExternalServiceException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Private constructor with error code, message, and cause.
     *
     * @param errorCode Specific error code
     * @param message Error message
     * @param cause Original exception
     */
    private ExternalServiceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.BAD_GATEWAY, cause);
    }
}
