package com.morawski.dev.backend.util;

import java.util.regex.Pattern;

/**
 * Utility class for string operations, formatting, and validation.
 * Provides helper methods for email masking, URL validation, and text formatting.
 */
public final class StringUtils {

    /**
     * Email validation pattern (RFC 5322 simplified).
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Password strength pattern: minimum 8 chars, must contain uppercase, lowercase, number, special char.
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    /**
     * URL validation pattern (HTTP/HTTPS).
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
    );

    /**
     * UUID pattern validation.
     */
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Masks email address for logging (GDPR compliance).
     * Example: "user@example.com" -> "u***@example.com"
     *
     * @param email the email to mask
     * @return masked email string
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() == 1) {
            return localPart + "***" + domain;
        }

        return localPart.charAt(0) + "***" + domain;
    }

    /**
     * Truncates text to specified length with ellipsis.
     *
     * @param text      the text to truncate
     * @param maxLength maximum length (including ellipsis)
     * @return truncated text
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        if (maxLength <= 3) {
            return "...";
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Masks review content for logging (privacy protection).
     * Shows only first 50 characters with ellipsis.
     *
     * @param content the review content
     * @return masked content
     */
    public static String maskReviewContent(String content) {
        return truncate(content, 50);
    }

    /**
     * Validates email format.
     *
     * @param email the email to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates password strength.
     * Must contain: uppercase, lowercase, number, special character, minimum 8 chars.
     *
     * @param password the password to validate
     * @return true if password meets strength requirements
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validates URL format (HTTP/HTTPS only).
     *
     * @param url the URL to validate
     * @return true if valid URL format
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * Validates UUID format.
     *
     * @param uuid the UUID string to validate
     * @return true if valid UUID format
     */
    public static boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid.trim()).matches();
    }

    /**
     * Checks if string is null or empty (after trimming).
     *
     * @param str the string to check
     * @return true if null or empty
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if string is not null and not empty (after trimming).
     *
     * @param str the string to check
     * @return true if not blank
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Safely trims a string (returns empty string if null).
     *
     * @param str the string to trim
     * @return trimmed string or empty string if null
     */
    public static String safeTrim(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * Normalizes whitespace (replaces multiple spaces with single space).
     *
     * @param text the text to normalize
     * @return normalized text
     */
    public static String normalizeWhitespace(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Capitalizes first letter of a string.
     *
     * @param str the string to capitalize
     * @return capitalized string
     */
    public static String capitalize(String str) {
        if (isBlank(str)) {
            return str;
        }
        String trimmed = str.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Generates a safe filename from a brand name (removes special characters).
     *
     * @param brandName the brand name
     * @return safe filename string
     */
    public static String toSafeFilename(String brandName) {
        if (isBlank(brandName)) {
            return "export";
        }
        return brandName.trim()
            .replaceAll("[^a-zA-Z0-9-_]", "_")
            .replaceAll("_+", "_")
            .toLowerCase();
    }

    /**
     * Formats a count with "+" suffix for large numbers.
     * Example: 999 -> "999", 1000 -> "999+"
     *
     * @param count the count to format
     * @param max   maximum to display before using "+"
     * @return formatted count string
     */
    public static String formatCount(int count, int max) {
        return count > max ? max + "+" : String.valueOf(count);
    }

    /**
     * Validates external profile ID format (non-empty alphanumeric).
     *
     * @param externalId the external ID to validate
     * @return true if valid
     */
    public static boolean isValidExternalId(String externalId) {
        if (isBlank(externalId)) {
            return false;
        }
        return externalId.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Extracts Google Place ID from Google Maps URL.
     * Example: "https://www.google.com/maps/place/.../@50.0647,19.9450,17z/data=!4m5!3m4!1s0x47165b0ae0b0c0c3:0x123456789"
     *
     * @param url the Google Maps URL
     * @return extracted Place ID or null if not found
     */
    public static String extractGooglePlaceId(String url) {
        if (isBlank(url)) {
            return null;
        }

        // Simple extraction - look for pattern like "1s0x..." or "ChIJ..."
        // This is a simplified version - production would need more robust parsing
        if (url.contains("ChIJ")) {
            int start = url.indexOf("ChIJ");
            int end = url.indexOf("!", start);
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, Math.min(start + 27, end)); // ChIJ IDs are ~27 chars
        }

        return null;
    }

    /**
     * Formats percentage with 2 decimal places.
     *
     * @param value the decimal value (0.7368 -> 73.68%)
     * @return formatted percentage string
     */
    public static String formatPercentage(double value) {
        return String.format("%.2f%%", value * 100);
    }

    /**
     * Formats rating with 2 decimal places.
     *
     * @param rating the rating value
     * @return formatted rating string
     */
    public static String formatRating(double rating) {
        return String.format("%.2f", rating);
    }
}
