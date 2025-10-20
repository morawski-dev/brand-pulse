package com.morawski.dev.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard error response format for all API errors.
 * Provides consistent error structure across the application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Error code for programmatic identification
     */
    private String code;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private ZonedDateTime timestamp;

    /**
     * API path where the error occurred
     */
    private String path;

    /**
     * Additional error details (flexible key-value pairs)
     */
    private Map<String, Object> details;

    /**
     * Validation errors for field-level errors
     */
    private List<ValidationError> errors;
}
