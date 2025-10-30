package com.morawski.dev.backend.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 * Provides helper methods for date/time calculations, formatting, and timezone handling.
 */
public final class DateTimeUtils {

    /**
     * Central European Time zone used for CRON jobs and reporting.
     */
    public static final ZoneId CET_ZONE = ZoneId.of("Europe/Paris");

    /**
     * ISO 8601 date-time formatter for API responses.
     */
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Date-only formatter (yyyy-MM-dd) for date range queries.
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets current timestamp in UTC.
     *
     * @return current LocalDateTime in UTC
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Gets current date in UTC.
     *
     * @return current LocalDate in UTC
     */
    public static LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Gets current timestamp in CET timezone.
     *
     * @return current LocalDateTime in CET
     */
    public static LocalDateTime nowCet() {
        return LocalDateTime.now(CET_ZONE);
    }

    /**
     * Calculates the date 90 days ago from today (for initial review import).
     *
     * @return LocalDate 90 days before today
     */
    public static LocalDate get90DaysAgo() {
        return todayUtc().minusDays(90);
    }

    /**
     * Checks if a timestamp is within the last 24 hours (for rate limiting).
     *
     * @param timestamp the timestamp to check
     * @return true if timestamp is within last 24 hours
     */
    public static boolean isWithinLast24Hours(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }
        return timestamp.isAfter(nowUtc().minusHours(24));
    }

    /**
     * Calculates the next available time after a 24-hour cooldown.
     *
     * @param lastRefreshAt the last refresh timestamp
     * @return LocalDateTime when next refresh is allowed
     */
    public static LocalDateTime calculateNext24HourWindow(LocalDateTime lastRefreshAt) {
        if (lastRefreshAt == null) {
            return nowUtc();
        }
        return lastRefreshAt.plusHours(24);
    }

    /**
     * Calculates hours remaining until next available refresh.
     *
     * @param lastRefreshAt the last refresh timestamp
     * @return hours remaining (0 if refresh is already available)
     */
    public static long hoursUntilNextRefresh(LocalDateTime lastRefreshAt) {
        if (lastRefreshAt == null) {
            return 0;
        }
        LocalDateTime nextAvailable = calculateNext24HourWindow(lastRefreshAt);
        long hours = ChronoUnit.HOURS.between(nowUtc(), nextAvailable);
        return Math.max(0, hours);
    }

    /**
     * Validates date range (endDate must be after startDate).
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return true if range is valid
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }

    /**
     * Converts LocalDateTime to ISO 8601 string for API responses.
     *
     * @param dateTime the datetime to format
     * @return ISO 8601 formatted string
     */
    public static String toIsoString(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneOffset.UTC).format(ISO_FORMATTER);
    }

    /**
     * Converts LocalDate to ISO 8601 date string (yyyy-MM-dd).
     *
     * @param date the date to format
     * @return ISO 8601 date string
     */
    public static String toIsoDateString(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Calculates duration between two timestamps in minutes.
     *
     * @param start start timestamp
     * @param end   end timestamp
     * @return duration in minutes
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Checks if a token has expired based on expiration timestamp.
     *
     * @param expiresAt the expiration timestamp
     * @return true if token has expired
     */
    public static boolean isExpired(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return true;
        }
        return expiresAt.isBefore(nowUtc());
    }

    /**
     * Generates expiration timestamp for JWT tokens.
     *
     * @param expirationMinutes minutes until expiration
     * @return expiration timestamp
     */
    public static LocalDateTime getJwtExpiration(int expirationMinutes) {
        return nowUtc().plusMinutes(expirationMinutes);
    }

    /**
     * Gets the start of day for a given date in UTC.
     *
     * @param date the date
     * @return LocalDateTime at start of day (00:00:00)
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * Gets the end of day for a given date in UTC.
     *
     * @param date the date
     * @return LocalDateTime at end of day (23:59:59.999999999)
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX);
    }

    /**
     * Gets the scheduled time for daily CRON job (3:00 AM CET).
     *
     * @return LocalDateTime for next 3:00 AM CET
     */
    public static LocalDateTime getNextDailySyncTime() {
        ZonedDateTime now = ZonedDateTime.now(CET_ZONE);
        ZonedDateTime next3AM = now.withHour(3).withMinute(0).withSecond(0).withNano(0);

        // If it's already past 3 AM today, schedule for tomorrow
        if (now.isAfter(next3AM)) {
            next3AM = next3AM.plusDays(1);
        }

        return next3AM.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Gets the start of the week (Monday) for a given date.
     *
     * @param date the reference date
     * @return LocalDate of the Monday in that week
     */
    public static LocalDate getStartOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(DayOfWeek.MONDAY);
    }

    /**
     * Gets the end of the week (Sunday) for a given date.
     *
     * @param date the reference date
     * @return LocalDate of the Sunday in that week
     */
    public static LocalDate getEndOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(DayOfWeek.SUNDAY);
    }
}
