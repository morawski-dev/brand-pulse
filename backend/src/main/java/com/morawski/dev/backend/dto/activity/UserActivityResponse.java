package com.morawski.dev.backend.dto.activity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Response DTO for user activity log entry.
 * Maps to user_activity_log table.
 * Used for success metrics tracking (Time to Value, Activation, Retention).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserActivityResponse {

    /**
     * Activity ID from user_activity_log.id
     */
    private Long activityId;

    /**
     * Activity type from user_activity_log.activity_type
     */
    private ActivityType activityType;

    /**
     * Activity occurrence timestamp from user_activity_log.occurred_at
     */
    private ZonedDateTime occurredAt;

    /**
     * Additional activity metadata from user_activity_log.metadata (JSONB)
     * Flexible key-value pairs for event-specific data
     */
    private Map<String, Object> metadata;
}
