package com.morawski.dev.backend.dto.common;

/**
 * Authentication method for review sources.
 * Maps to review_sources.auth_method in database.
 */
public enum AuthMethod {
    API,
    SCRAPING
}
