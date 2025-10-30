package com.morawski.dev.backend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Utility class for common validation logic.
 * Provides reusable validation methods for DTOs and business logic.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates pagination parameters.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @param maxSize maximum allowed page size
     * @return true if valid
     */
    public static boolean isValidPagination(int page, int size, int maxSize) {
        return page >= 0 && size > 0 && size <= maxSize;
    }

    /**
     * Validates date range (end must be after or equal to start).
     *
     * @param startDate start date
     * @param endDate end date
     * @return true if valid range
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }

    /**
     * Validates date range with timestamps.
     *
     * @param startDateTime start timestamp
     * @param endDateTime end timestamp
     * @return true if valid range
     */
    public static boolean isValidDateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return false;
        }
        return !endDateTime.isBefore(startDateTime);
    }

    /**
     * Validates star rating (must be 1-5).
     *
     * @param rating the rating value
     * @return true if valid rating
     */
    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    /**
     * Validates sentiment confidence score (0.0 - 1.0).
     *
     * @param confidence the confidence score
     * @return true if valid confidence
     */
    public static boolean isValidConfidence(double confidence) {
        return confidence >= 0.0 && confidence <= 1.0;
    }

    /**
     * Validates percentage value (0.0 - 100.0).
     *
     * @param percentage the percentage value
     * @return true if valid percentage
     */
    public static boolean isValidPercentage(double percentage) {
        return percentage >= 0.0 && percentage <= 100.0;
    }

    /**
     * Validates brand name length and content.
     *
     * @param name the brand name
     * @param maxLength maximum allowed length
     * @return true if valid brand name
     */
    public static boolean isValidBrandName(String name, int maxLength) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        return name.trim().length() <= maxLength;
    }

    /**
     * Validates password confirmation match.
     *
     * @param password the password
     * @param confirmPassword the confirmation password
     * @return true if passwords match
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * Validates JWT expiration minutes.
     *
     * @param minutes expiration in minutes
     * @return true if valid (between 1 and 1440 minutes / 24 hours)
     */
    public static boolean isValidJwtExpiration(int minutes) {
        return minutes > 0 && minutes <= 1440; // Max 24 hours
    }

    /**
     * Validates that a value is positive.
     *
     * @param value the value to check
     * @return true if positive
     */
    public static boolean isPositive(int value) {
        return value > 0;
    }

    /**
     * Validates that a value is non-negative.
     *
     * @param value the value to check
     * @return true if non-negative
     */
    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    /**
     * Validates that a Long ID is valid (not null and positive).
     *
     * @param id the ID to validate
     * @return true if valid ID
     */
    public static boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    /**
     * Validates sync job type.
     *
     * @param jobType the job type string
     * @return true if valid job type (INITIAL, SCHEDULED, MANUAL)
     */
    public static boolean isValidJobType(String jobType) {
        if (StringUtils.isBlank(jobType)) {
            return false;
        }
        return jobType.matches("^(INITIAL|SCHEDULED|MANUAL)$");
    }

    /**
     * Validates sync status.
     *
     * @param status the sync status string
     * @return true if valid status (PENDING, IN_PROGRESS, COMPLETED, FAILED)
     */
    public static boolean isValidSyncStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return false;
        }
        return status.matches("^(PENDING|IN_PROGRESS|COMPLETED|FAILED)$");
    }

    /**
     * Validates source type.
     *
     * @param sourceType the source type string
     * @return true if valid source type (GOOGLE, FACEBOOK, TRUSTPILOT)
     */
    public static boolean isValidSourceType(String sourceType) {
        if (StringUtils.isBlank(sourceType)) {
            return false;
        }
        return sourceType.matches("^(GOOGLE|FACEBOOK|TRUSTPILOT)$");
    }

    /**
     * Validates sentiment value.
     *
     * @param sentiment the sentiment string
     * @return true if valid sentiment (POSITIVE, NEGATIVE, NEUTRAL)
     */
    public static boolean isValidSentiment(String sentiment) {
        if (StringUtils.isBlank(sentiment)) {
            return false;
        }
        return sentiment.matches("^(POSITIVE|NEGATIVE|NEUTRAL)$");
    }

    /**
     * Validates auth method.
     *
     * @param authMethod the auth method string
     * @return true if valid auth method (API, SCRAPING)
     */
    public static boolean isValidAuthMethod(String authMethod) {
        if (StringUtils.isBlank(authMethod)) {
            return false;
        }
        return authMethod.matches("^(API|SCRAPING)$");
    }

    /**
     * Validates plan type.
     *
     * @param planType the plan type string
     * @return true if valid plan type (FREE, BASIC, PREMIUM, ENTERPRISE)
     */
    public static boolean isValidPlanType(String planType) {
        if (StringUtils.isBlank(planType)) {
            return false;
        }
        return planType.matches("^(FREE|BASIC|PREMIUM|ENTERPRISE)$");
    }

    /**
     * Validates that a date is not in the future.
     *
     * @param date the date to validate
     * @return true if date is today or in the past
     */
    public static boolean isNotFuture(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isAfter(DateTimeUtils.todayUtc());
    }

    /**
     * Validates that a timestamp is not in the future.
     *
     * @param dateTime the timestamp to validate
     * @return true if timestamp is now or in the past
     */
    public static boolean isNotFuture(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return !dateTime.isAfter(DateTimeUtils.nowUtc());
    }
}
