package com.morawski.dev.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single field validation error.
 * Used within ErrorResponse for validation failures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

    /**
     * Field name that failed validation
     */
    private String field;

    /**
     * The value that was rejected
     */
    private Object rejectedValue;

    /**
     * Validation error message
     */
    private String message;
}
