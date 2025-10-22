package com.morawski.dev.backend.dto.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * Request DTO for updating review source information.
 * Updates review_sources table fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewSourceRequest {

    /**
     * Update active status (pause/resume syncing)
     */
    private Boolean isActive;

    /**
     * Update profile URL
     */
    @URL(message = "Invalid URL format")
    private String profileUrl;
}
