package com.morawski.dev.backend.util;

/**
 * Application-wide constants.
 * Centralizes all constant values used across the BrandPulse application.
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ===================================
    // API Configuration
    // ===================================

    /**
     * API base path for all endpoints.
     */
    public static final String API_BASE_PATH = "/api/v1";

    /**
     * Current API version.
     */
    public static final String API_VERSION = "v1";

    // ===================================
    // Pagination Defaults
    // ===================================

    /**
     * Default page size for paginated endpoints.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size for paginated endpoints.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Default page number (0-indexed).
     */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // ===================================
    // Rate Limiting
    // ===================================

    /**
     * Manual refresh cooldown period in hours.
     */
    public static final int MANUAL_REFRESH_COOLDOWN_HOURS = 24;

    /**
     * Maximum login attempts per 15 minutes.
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * Maximum registration attempts per IP per hour.
     */
    public static final int MAX_REGISTRATION_ATTEMPTS = 3;

    /**
     * Global rate limit: requests per user per hour.
     */
    public static final int GLOBAL_RATE_LIMIT_PER_USER = 1000;

    /**
     * Global rate limit: requests per IP per hour (unauthenticated).
     */
    public static final int GLOBAL_RATE_LIMIT_PER_IP = 5000;

    // ===================================
    // Plan Limits
    // ===================================

    /**
     * Free plan: maximum review sources allowed.
     */
    public static final int FREE_PLAN_MAX_SOURCES = 1;

    /**
     * MVP: maximum brands per user.
     */
    public static final int MAX_BRANDS_PER_USER = 1;

    // ===================================
    // Time Constants
    // ===================================

    /**
     * Default JWT expiration in minutes.
     */
    public static final int JWT_EXPIRATION_MINUTES = 60;

    /**
     * Password reset token expiration in hours.
     */
    public static final int PASSWORD_RESET_TOKEN_EXPIRATION_HOURS = 1;

    /**
     * AI summary validity period in hours.
     */
    public static final int AI_SUMMARY_VALIDITY_HOURS = 24;

    /**
     * Initial import period: days to import from past.
     */
    public static final int INITIAL_IMPORT_DAYS = 90;

    /**
     * Cache TTL in minutes.
     */
    public static final int CACHE_TTL_MINUTES = 10;

    /**
     * Dashboard cache TTL in minutes.
     */
    public static final int DASHBOARD_CACHE_TTL_MINUTES = 10;

    /**
     * AI summary cache TTL in hours.
     */
    public static final int AI_SUMMARY_CACHE_TTL_HOURS = 24;

    // ===================================
    // Validation Constraints
    // ===================================

    /**
     * Minimum password length.
     */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Maximum password length.
     */
    public static final int MAX_PASSWORD_LENGTH = 100;

    /**
     * Maximum email length.
     */
    public static final int MAX_EMAIL_LENGTH = 255;

    /**
     * Maximum brand name length.
     */
    public static final int MAX_BRAND_NAME_LENGTH = 255;

    /**
     * Maximum external profile ID length.
     */
    public static final int MAX_EXTERNAL_ID_LENGTH = 255;

    /**
     * Maximum URL length.
     */
    public static final int MAX_URL_LENGTH = 2048;

    /**
     * Maximum review content length.
     */
    public static final int MAX_REVIEW_CONTENT_LENGTH = 5000;

    // ===================================
    // Performance Targets
    // ===================================

    /**
     * Dashboard load time target in seconds.
     */
    public static final int DASHBOARD_LOAD_TIME_TARGET_SECONDS = 4;

    /**
     * Review list load time target in seconds.
     */
    public static final int REVIEW_LIST_LOAD_TIME_TARGET_SECONDS = 2;

    /**
     * Single resource GET time target in milliseconds.
     */
    public static final int SINGLE_GET_TIME_TARGET_MS = 500;

    // ===================================
    // Success Metrics Thresholds
    // ===================================

    /**
     * Time to Value target: minutes to configure first source.
     */
    public static final int TIME_TO_VALUE_TARGET_MINUTES = 10;

    /**
     * Activation target: percentage of users configuring source within 7 days.
     */
    public static final double ACTIVATION_TARGET_PERCENTAGE = 60.0;

    /**
     * Retention target: percentage with 3+ logins in 4 weeks.
     */
    public static final double RETENTION_TARGET_PERCENTAGE = 35.0;

    /**
     * AI accuracy target: percentage of correct sentiment classifications.
     */
    public static final double AI_ACCURACY_TARGET_PERCENTAGE = 75.0;

    // ===================================
    // Error Codes
    // ===================================

    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    public static final String ERROR_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ERROR_EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String ERROR_INVALID_TOKEN = "INVALID_TOKEN";
    public static final String ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String ERROR_EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String ERROR_DUPLICATE_SOURCE = "DUPLICATE_SOURCE";
    public static final String ERROR_RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ERROR_PLAN_LIMIT_EXCEEDED = "PLAN_LIMIT_EXCEEDED";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String ERROR_BRAND_LIMIT_EXCEEDED = "BRAND_LIMIT_EXCEEDED";
    public static final String ERROR_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS";
    public static final String ERROR_INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String ERROR_GOOGLE_API = "GOOGLE_API_ERROR";
    public static final String ERROR_AI_SERVICE_UNAVAILABLE = "AI_SERVICE_UNAVAILABLE";
    public static final String ERROR_SCRAPER_FAILED = "SCRAPER_FAILED";

    // ===================================
    // Activity Types (User Activity Log)
    // ===================================

    public static final String ACTIVITY_USER_REGISTERED = "USER_REGISTERED";
    public static final String ACTIVITY_LOGIN = "LOGIN";
    public static final String ACTIVITY_LOGOUT = "LOGOUT";
    public static final String ACTIVITY_VIEW_DASHBOARD = "VIEW_DASHBOARD";
    public static final String ACTIVITY_FILTER_APPLIED = "FILTER_APPLIED";
    public static final String ACTIVITY_SENTIMENT_CORRECTED = "SENTIMENT_CORRECTED";
    public static final String ACTIVITY_SOURCE_CONFIGURED = "SOURCE_CONFIGURED";
    public static final String ACTIVITY_SOURCE_ADDED = "SOURCE_ADDED";
    public static final String ACTIVITY_SOURCE_DELETED = "SOURCE_DELETED";
    public static final String ACTIVITY_MANUAL_REFRESH_TRIGGERED = "MANUAL_REFRESH_TRIGGERED";
    public static final String ACTIVITY_FIRST_SOURCE_CONFIGURED_SUCCESSFULLY = "FIRST_SOURCE_CONFIGURED_SUCCESSFULLY";

    // ===================================
    // Cache Keys Prefixes
    // ===================================

    public static final String CACHE_PREFIX_DASHBOARD = "dashboard:brand:";
    public static final String CACHE_PREFIX_SUMMARY = "summary:source:";
    public static final String CACHE_PREFIX_REVIEWS = "reviews:brand:";

    // ===================================
    // Job Types (Sync Jobs)
    // ===================================

    public static final String JOB_TYPE_INITIAL = "INITIAL";
    public static final String JOB_TYPE_SCHEDULED = "SCHEDULED";
    public static final String JOB_TYPE_MANUAL = "MANUAL";

    // ===================================
    // Sync Status
    // ===================================

    public static final String SYNC_STATUS_PENDING = "PENDING";
    public static final String SYNC_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String SYNC_STATUS_COMPLETED = "COMPLETED";
    public static final String SYNC_STATUS_FAILED = "FAILED";

    // ===================================
    // Sentiment Types
    // ===================================

    public static final String SENTIMENT_POSITIVE = "POSITIVE";
    public static final String SENTIMENT_NEGATIVE = "NEGATIVE";
    public static final String SENTIMENT_NEUTRAL = "NEUTRAL";

    // ===================================
    // Source Types
    // ===================================

    public static final String SOURCE_TYPE_GOOGLE = "GOOGLE";
    public static final String SOURCE_TYPE_FACEBOOK = "FACEBOOK";
    public static final String SOURCE_TYPE_TRUSTPILOT = "TRUSTPILOT";

    // ===================================
    // Auth Methods
    // ===================================

    public static final String AUTH_METHOD_API = "API";
    public static final String AUTH_METHOD_SCRAPING = "SCRAPING";

    // ===================================
    // Plan Types
    // ===================================

    public static final String PLAN_TYPE_FREE = "FREE";
    public static final String PLAN_TYPE_BASIC = "BASIC";
    public static final String PLAN_TYPE_PREMIUM = "PREMIUM";
    public static final String PLAN_TYPE_ENTERPRISE = "ENTERPRISE";

    // ===================================
    // Change Reasons (Sentiment Changes)
    // ===================================

    public static final String CHANGE_REASON_AI_INITIAL = "AI_INITIAL";
    public static final String CHANGE_REASON_USER_CORRECTION = "USER_CORRECTION";
    public static final String CHANGE_REASON_REPROCESSING = "REPROCESSING";

    // ===================================
    // HTTP Headers
    // ===================================

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_BEARER_PREFIX = "Bearer ";
    public static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    // ===================================
    // CRON Expressions
    // ===================================

    /**
     * Daily sync at 3:00 AM CET.
     * Cron: second, minute, hour, day, month, weekday
     */
    public static final String CRON_DAILY_SYNC = "0 0 3 * * ?";

    /**
     * Weekly email reports on Sunday at 6:00 AM CET.
     */
    public static final String CRON_WEEKLY_EMAIL = "0 0 6 ? * SUN";

    // ===================================
    // Email Templates
    // ===================================

    public static final String EMAIL_TEMPLATE_WELCOME = "welcome";
    public static final String EMAIL_TEMPLATE_PASSWORD_RESET = "password-reset";
    public static final String EMAIL_TEMPLATE_WEEKLY_SUMMARY = "weekly-summary";
    public static final String EMAIL_TEMPLATE_EMAIL_VERIFICATION = "email-verification";

    // ===================================
    // AI Configuration
    // ===================================

    /**
     * Default AI model for sentiment analysis.
     */
    public static final String AI_MODEL_SENTIMENT = "anthropic/claude-3-haiku";

    /**
     * Default AI model for text summarization.
     */
    public static final String AI_MODEL_SUMMARY = "anthropic/claude-3-haiku";

    /**
     * Maximum tokens for AI summary generation.
     */
    public static final int AI_SUMMARY_MAX_TOKENS = 500;

    /**
     * Number of recent reviews to include in AI summary context.
     */
    public static final int AI_SUMMARY_REVIEW_COUNT = 100;

    // ===================================
    // Security
    // ===================================

    /**
     * BCrypt strength (number of rounds).
     */
    public static final int BCRYPT_STRENGTH = 10;

    /**
     * AES encryption key size in bits.
     */
    public static final int AES_KEY_SIZE = 256;

    // ===================================
    // Monitoring
    // ===================================

    /**
     * Slow query threshold in milliseconds.
     */
    public static final int SLOW_QUERY_THRESHOLD_MS = 2000;

    /**
     * Health check endpoint.
     */
    public static final String HEALTH_ENDPOINT = "/actuator/health";

    /**
     * Metrics endpoint (Prometheus).
     */
    public static final String METRICS_ENDPOINT = "/actuator/prometheus";
}
