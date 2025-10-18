package com.morawski.dev.backend.exception;

import com.morawski.dev.backend.dto.common.ValidationError;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when request data fails validation.
 * Results in HTTP 400 Bad Request response.
 *
 * Used when:
 * - Request body validation fails (JSR-380 Bean Validation)
 * - Custom validation rules fail
 * - Password confirmation doesn't match
 * - Invalid date ranges
 * - Invalid enum values
 *
 * Example usage:
 * <pre>
 * if (!password.equals(confirmPassword)) {
 *     throw new ValidationException("Passwords must match")
 *         .addFieldError("confirmPassword", confirmPassword, "Passwords must match");
 * }
 * </pre>
 */
public class ValidationException extends BrandPulseException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";
    private static final String DEFAULT_MESSAGE = "Request validation failed";

    private final List<ValidationError> fieldErrors = new ArrayList<>();

    /**
     * Constructor with default message.
     */
    public ValidationException() {
        super(ERROR_CODE, DEFAULT_MESSAGE, HttpStatus.BAD_REQUEST);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Custom error message
     */
    public ValidationException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Add a field-level validation error.
     *
     * @param field Field name
     * @param rejectedValue Value that was rejected
     * @param message Validation error message
     * @return This exception instance for method chaining
     */
    public ValidationException addFieldError(String field, Object rejectedValue, String message) {
        fieldErrors.add(ValidationError.builder()
            .field(field)
            .rejectedValue(rejectedValue)
            .message(message)
            .build());
        return this;
    }

    /**
     * Add a field-level validation error without rejected value.
     *
     * @param field Field name
     * @param message Validation error message
     * @return This exception instance for method chaining
     */
    public ValidationException addFieldError(String field, String message) {
        fieldErrors.add(ValidationError.builder()
            .field(field)
            .message(message)
            .build());
        return this;
    }

    /**
     * Get all field errors.
     *
     * @return List of field errors
     */
    public List<ValidationError> getFieldErrors() {
        return new ArrayList<>(fieldErrors);
    }

    /**
     * Check if there are any field errors.
     *
     * @return true if field errors exist
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
