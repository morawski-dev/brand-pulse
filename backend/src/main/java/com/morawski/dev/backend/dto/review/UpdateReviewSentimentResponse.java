package com.morawski.dev.backend.dto.review;

import com.morawski.dev.backend.dto.common.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for sentiment update operation (US-007).
 * Returns updated sentiment and audit trail information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewSentimentResponse {

    /**
     * Review ID from reviews.id
     */
    private Long reviewId;

    /**
     * New sentiment value from reviews.sentiment
     */
    private Sentiment sentiment;

    /**
     * Previous sentiment value (before update)
     */
    private Sentiment previousSentiment;

    /**
     * Update timestamp from reviews.updated_at
     */
    private ZonedDateTime updatedAt;

    /**
     * Sentiment change audit record ID from sentiment_changes.id
     */
    private Long sentimentChangeId;
}
