package com.morawski.dev.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception class for all BrandPulse application exceptions.
 * Provides common properties for error handling and response generation.
 *
 * All custom exceptions should extend this class to ensure consistent
 * error responses across the API.
 */
@Getter
public abstract class BrandPulseException extends RuntimeException {

    /**
     * Error code for API response (e.g., "RESOURCE_NOT_FOUND", "INVALID_CREDENTIALS").
     * Used to identify the specific error type on the client side.
     */
    private final String errorCode;

    /**
     * HTTP status code to return (e.g., 404, 401, 403).
     */
    private final HttpStatus httpStatus;

    /**
     * Additional details about the error (optional).
     * Can contain context-specific information like field names, limits, etc.
     */
    private final Map<String, Object> details;

    /**
     * Constructor with error code, message, and HTTP status.
     *
     * @param errorCode Unique error code identifier
     * @param message Human-readable error message
     * @param httpStatus HTTP status code for response
     */
    protected BrandPulseException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with error code, message, HTTP status, and cause.
     *
     * @param errorCode Unique error code identifier
     * @param message Human-readable error message
     * @param httpStatus HTTP status code for response
     * @param cause The underlying cause of this exception
     */
    protected BrandPulseException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = new HashMap<>();
    }

    /**
     * Add additional detail to the error response.
     *
     * @param key Detail key
     * @param value Detail value
     * @return This exception instance for method chaining
     */
    public BrandPulseException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    /**
     * Add multiple details to the error response.
     *
     * @param details Map of details to add
     * @return This exception instance for method chaining
     */
    public BrandPulseException addDetails(Map<String, Object> details) {
        this.details.putAll(details);
        return this;
    }
}
