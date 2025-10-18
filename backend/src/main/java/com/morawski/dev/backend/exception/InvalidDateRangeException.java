package com.morawski.dev.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Exception thrown when date range parameters are invalid.
 * Results in HTTP 400 Bad Request response.
 *
 * Used when:
 * - endDate is before startDate
 * - Date range exceeds maximum allowed (e.g., 1 year)
 * - Invalid date format provided
 *
 * Example usage:
 * <pre>
 * if (endDate.isBefore(startDate)) {
 *     throw new InvalidDateRangeException(startDate, endDate);
 * }
 * </pre>
 */
public class InvalidDateRangeException extends BrandPulseException {

    private static final String ERROR_CODE = "INVALID_DATE_RANGE";

    /**
     * Constructor with start and end dates.
     *
     * @param startDate Start date
     * @param endDate End date
     */
    public InvalidDateRangeException(LocalDate startDate, LocalDate endDate) {
        super(
            ERROR_CODE,
            String.format("Invalid date range: endDate (%s) must be after startDate (%s)", endDate, startDate),
            HttpStatus.BAD_REQUEST
        );
        addDetail("startDate", startDate);
        addDetail("endDate", endDate);
    }

    /**
     * Constructor with custom message.
     *
     * @param message Error message
     */
    public InvalidDateRangeException(String message) {
        super(ERROR_CODE, message, HttpStatus.BAD_REQUEST);
    }
}
