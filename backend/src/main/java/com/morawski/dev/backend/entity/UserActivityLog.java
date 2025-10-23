package com.morawski.dev.backend.entity;

import com.morawski.dev.backend.dto.common.ActivityType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

/**
 * Entity representing a user activity event for analytics and success metrics tracking.
 *
 * Used for calculating success metrics:
 * - Time to Value: USER_REGISTERED → FIRST_SOURCE_CONFIGURED_SUCCESSFULLY < 10 minutes (90%)
 * - Activation: SOURCE_CONFIGURED within 7 days (60%)
 * - Retention: 3+ LOGIN events in first 4 weeks (35%)
 * - AI Accuracy: Track SENTIMENT_CORRECTED events
 *
 * Activity Types:
 * - USER_REGISTERED, LOGIN, LOGOUT
 * - VIEW_DASHBOARD, FILTER_APPLIED
 * - SENTIMENT_CORRECTED
 * - SOURCE_CONFIGURED, SOURCE_ADDED, SOURCE_DELETED
 * - MANUAL_REFRESH_TRIGGERED
 * - FIRST_SOURCE_CONFIGURED_SUCCESSFULLY
 */
@Entity
@Table(name = "user_activity_log", indexes = {
        @Index(name = "idx_user_activity_user_id", columnList = "user_id"),
        @Index(name = "idx_user_activity_type", columnList = "activity_type, occurred_at"),
        @Index(name = "idx_user_activity_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_user_activity_registration", columnList = "user_id, activity_type, occurred_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Activity type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    /**
     * When the activity occurred.
     */
    @NotNull(message = "Occurred at is required")
    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    /**
     * Additional metadata stored as JSON.
     * Examples:
     * - SENTIMENT_CORRECTED: {"reviewId": 123, "oldSentiment": "NEUTRAL", "newSentiment": "NEGATIVE"}
     * - VIEW_DASHBOARD: {"brandId": 1, "filters": {"sentiment": "NEGATIVE"}}
     * - SOURCE_CONFIGURED: {"sourceId": 1, "sourceType": "GOOGLE"}
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Many-to-one relationship with User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Check if this is a registration event.
     */
    public boolean isRegistration() {
        return activityType == ActivityType.USER_REGISTERED;
    }

    /**
     * Check if this is a login event.
     */
    public boolean isLogin() {
        return activityType == ActivityType.LOGIN;
    }

    /**
     * Check if this is a first source configuration event.
     */
    public boolean isFirstSourceConfigured() {
        return activityType == ActivityType.FIRST_SOURCE_CONFIGURED_SUCCESSFULLY;
    }

    /**
     * Get metadata value by key.
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Check if metadata contains key.
     */
    public boolean hasMetadata(String key) {
        return metadata != null && metadata.containsKey(key);
    }
}
