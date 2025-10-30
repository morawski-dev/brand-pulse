package com.morawski.dev.backend.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for building standardized API responses.
 * Ensures consistent response format across all endpoints.
 */
public final class ResponseBuilder {

    private ResponseBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds a standard error response.
     *
     * @param code    error code (e.g., "VALIDATION_ERROR")
     * @param message human-readable error message
     * @param path    the request path
     * @return error response map
     */
    public static Map<String, Object> buildErrorResponse(String code, String message, String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", DateTimeUtils.toIsoString(DateTimeUtils.nowUtc()));
        response.put("path", path);
        return response;
    }

    /**
     * Builds a validation error response with field-level errors.
     *
     * @param message human-readable error message
     * @param path    the request path
     * @param errors  list of field validation errors
     * @return validation error response map
     */
    public static Map<String, Object> buildValidationErrorResponse(String message, String path, List<Map<String, String>> errors) {
        Map<String, Object> response = buildErrorResponse(Constants.ERROR_VALIDATION, message, path);
        response.put("errors", errors);
        return response;
    }

    /**
     * Builds a field validation error entry.
     *
     * @param field         the field name
     * @param rejectedValue the rejected value
     * @param message       the validation error message
     * @return field error map
     */
    public static Map<String, String> buildFieldError(String field, String rejectedValue, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("field", field);
        error.put("rejectedValue", rejectedValue);
        error.put("message", message);
        return error;
    }

    /**
     * Builds a plan limit exceeded error response.
     *
     * @param message      human-readable message
     * @param path         the request path
     * @param currentCount current number of resources
     * @param maxAllowed   maximum allowed by plan
     * @param planType     user's current plan type
     * @return plan limit error response map
     */
    public static Map<String, Object> buildPlanLimitError(String message, String path, int currentCount, int maxAllowed, String planType) {
        Map<String, Object> response = buildErrorResponse(Constants.ERROR_PLAN_LIMIT_EXCEEDED, message, path);
        response.put("currentCount", currentCount);
        response.put("maxAllowed", maxAllowed);
        response.put("planType", planType);
        return response;
    }

    /**
     * Builds a rate limit exceeded error response.
     *
     * @param message           human-readable message
     * @param path              the request path
     * @param lastRefreshAt     timestamp of last refresh
     * @param nextAvailableAt   timestamp when next refresh is allowed
     * @param hoursRemaining    hours until next refresh
     * @return rate limit error response map
     */
    public static Map<String, Object> buildRateLimitError(String message, String path,
                                                           LocalDateTime lastRefreshAt,
                                                           LocalDateTime nextAvailableAt,
                                                           long hoursRemaining) {
        Map<String, Object> response = buildErrorResponse(Constants.ERROR_RATE_LIMIT_EXCEEDED, message, path);
        response.put("lastManualRefreshAt", DateTimeUtils.toIsoString(lastRefreshAt));
        response.put("nextAvailableAt", DateTimeUtils.toIsoString(nextAvailableAt));
        response.put("hoursRemaining", hoursRemaining);
        return response;
    }

    /**
     * Builds a simple success message response.
     *
     * @param message the success message
     * @return success response map
     */
    public static Map<String, Object> buildSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return response;
    }

    /**
     * Builds a pagination metadata object.
     *
     * @param currentPage current page number (0-indexed)
     * @param pageSize    items per page
     * @param totalItems  total number of items
     * @param totalPages  total number of pages
     * @return pagination metadata map
     */
    public static Map<String, Object> buildPaginationMetadata(int currentPage, int pageSize, long totalItems, int totalPages) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", currentPage);
        pagination.put("pageSize", pageSize);
        pagination.put("totalItems", totalItems);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", currentPage < totalPages - 1);
        pagination.put("hasPrevious", currentPage > 0);
        return pagination;
    }

    /**
     * Builds a response with data and pagination.
     *
     * @param data       the data to return
     * @param pagination pagination metadata
     * @return response map with data and pagination
     */
    public static Map<String, Object> buildPaginatedResponse(Object data, Map<String, Object> pagination) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("pagination", pagination);
        return response;
    }

    /**
     * Builds rate limit response headers.
     *
     * @param limit     total rate limit
     * @param remaining remaining requests
     * @param resetAt   timestamp when limit resets
     * @return headers map
     */
    public static Map<String, String> buildRateLimitHeaders(int limit, int remaining, long resetAt) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_RATE_LIMIT, String.valueOf(limit));
        headers.put(Constants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(remaining));
        headers.put(Constants.HEADER_RATE_LIMIT_RESET, String.valueOf(resetAt));
        return headers;
    }

    /**
     * Builds a sync job response with multiple jobs.
     *
     * @param message             success message
     * @param jobs                list of created sync jobs
     * @param nextAvailableAt     timestamp when next manual sync is allowed
     * @return sync response map
     */
    public static Map<String, Object> buildSyncResponse(String message, List<Map<String, Object>> jobs, LocalDateTime nextAvailableAt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("jobs", jobs);
        response.put("nextManualSyncAvailableAt", DateTimeUtils.toIsoString(nextAvailableAt));
        return response;
    }

    /**
     * Builds a health check response.
     *
     * @param status     health status (UP, DOWN)
     * @param components component health details
     * @return health response map
     */
    public static Map<String, Object> buildHealthResponse(String status, Map<String, Object> components) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("components", components);
        response.put("timestamp", DateTimeUtils.toIsoString(DateTimeUtils.nowUtc()));
        return response;
    }
}
