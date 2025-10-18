package com.morawski.dev.backend.exception;

import com.morawski.dev.backend.dto.common.ErrorResponse;
import com.morawski.dev.backend.dto.common.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST API endpoints.
 * Catches exceptions thrown by controllers and converts them to standardized error responses.
 *
 * Features:
 * - Consistent error response format across all endpoints
 * - Appropriate HTTP status codes
 * - Request path and timestamp in error response
 * - Detailed logging for debugging
 * - Security: Never expose sensitive information in error messages
 *
 * All error responses follow the structure defined in ErrorResponse and ValidationErrorResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all BrandPulse custom exceptions.
     * These exceptions already contain error code, message, and HTTP status.
     *
     * Special handling for ValidationException:
     * - If ValidationException has field errors, they are included in the response
     * - This provides consistent error format for both JSR-380 validation and custom validation
     */
    @ExceptionHandler(BrandPulseException.class)
    public ResponseEntity<ErrorResponse> handleBrandPulseException(
        BrandPulseException ex,
        HttpServletRequest request
    ) {
        log.warn("BrandPulse exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .details(ex.getDetails().isEmpty() ? null : ex.getDetails());

        // Handle ValidationException specially to include field errors
        if (ex instanceof ValidationException validationEx && validationEx.hasFieldErrors()) {
            builder.errors(validationEx.getFieldErrors());
        }

        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(builder.build());
    }

    /**
     * Handle Jakarta Bean Validation errors (JSR-380).
     * Triggered when @Valid annotation fails on request body.
     *
     * Example: @Valid @RequestBody CreateUserRequest request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        log.warn("Validation error on {}: {} field errors",
            request.getRequestURI(),
            ex.getBindingResult().getFieldErrorCount());

        List<ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::buildValidationError)
            .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Request validation failed")
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .errors(validationErrors)
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions (e.g., @NotNull on method parameters).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Constraint violation on {}: {}", request.getRequestURI(), ex.getMessage());

        List<ValidationError> validationErrors = ex.getConstraintViolations().stream()
            .map(violation -> ValidationError.builder()
                .field(violation.getPropertyPath().toString())
                .rejectedValue(violation.getInvalidValue())
                .message(violation.getMessage())
                .build())
            .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Validation constraint violated")
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .errors(validationErrors)
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handle type mismatch errors (e.g., passing "abc" for Long parameter).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        log.warn("Type mismatch on {}: parameter '{}' with value '{}'",
            request.getRequestURI(), ex.getName(), ex.getValue());

        String message = String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(),
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INVALID_PARAMETER_TYPE")
            .message(message)
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .details(Map.of(
                "parameter", ex.getName(),
                "rejectedValue", ex.getValue(),
                "expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
            ))
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handle malformed JSON in request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        log.warn("Malformed JSON on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("MALFORMED_JSON")
            .message("Invalid JSON format in request body")
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    /**
     * Handle Spring Security authentication exceptions.
     * Triggered when authentication fails (invalid credentials, expired token, etc.).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException ex,
        HttpServletRequest request
    ) {
        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("AUTHENTICATION_FAILED")
            .message("Authentication failed. Please check your credentials.")
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse);
    }

    /**
     * Handle Spring Security access denied exceptions (role/permission-based).
     * Triggered when authenticated user doesn't have required role/permission.
     *
     * Note: This handles org.springframework.security.access.AccessDeniedException,
     * NOT our custom ResourceAccessDeniedException (which handles resource ownership).
     * ResourceAccessDeniedException is handled by handleBrandPulseException().
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
        AccessDeniedException ex,
        HttpServletRequest request
    ) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("ACCESS_DENIED")
            .message("You don't have permission to access this resource")
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(errorResponse);
    }

    /**
     * Handle 404 Not Found when no handler is found for the request.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
        NoHandlerFoundException ex,
        HttpServletRequest request
    ) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("ENDPOINT_NOT_FOUND")
            .message(String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }

    /**
     * Catch-all handler for any unhandled exceptions.
     * Returns 500 Internal Server Error without exposing sensitive details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred. Please try again later.")
            .timestamp(ZonedDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    /**
     * Helper method to build ValidationError from Spring's FieldError.
     */
    private ValidationError buildValidationError(FieldError fieldError) {
        return ValidationError.builder()
            .field(fieldError.getField())
            .rejectedValue(fieldError.getRejectedValue())
            .message(fieldError.getDefaultMessage())
            .build();
    }
}
