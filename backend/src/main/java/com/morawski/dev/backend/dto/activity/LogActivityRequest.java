package com.morawski.dev.backend.dto.activity;

import com.morawski.dev.backend.dto.common.ActivityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Request DTO for logging user activity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogActivityRequest {

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    private Instant occurredAt;

    private Map<String, Object> metadata;
}
