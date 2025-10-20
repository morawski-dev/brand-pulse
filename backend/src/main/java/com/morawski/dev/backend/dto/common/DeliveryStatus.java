package com.morawski.dev.backend.dto.common;

/**
 * Email delivery status.
 * Maps to email_reports.delivery_status in database.
 */
public enum DeliveryStatus {
    SENT,
    DELIVERED,
    BOUNCED,
    FAILED
}
