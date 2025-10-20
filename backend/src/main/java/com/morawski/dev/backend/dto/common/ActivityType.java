package com.morawski.dev.backend.dto.common;

/**
 * User activity types for analytics tracking.
 * Maps to user_activity_log.activity_type in database.
 */
public enum ActivityType {
    USER_REGISTERED,
    LOGIN,
    LOGOUT,
    VIEW_DASHBOARD,
    FILTER_APPLIED,
    SENTIMENT_CORRECTED,
    SOURCE_CONFIGURED,
    SOURCE_ADDED,
    SOURCE_DELETED,
    MANUAL_REFRESH_TRIGGERED,
    FIRST_SOURCE_CONFIGURED_SUCCESSFULLY
}
