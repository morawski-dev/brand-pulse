package com.morawski.dev.backend.dto.common;

/**
 * Reason for sentiment change.
 * Maps to sentiment_changes.change_reason in database.
 */
public enum ChangeReason {
    AI_INITIAL,
    USER_CORRECTION,
    AI_REANALYSIS
}
