package com.morawski.dev.backend.dto.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.ChangeReason;
import com.morawski.dev.backend.dto.common.Sentiment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for sentiment change history.
 * Maps to sentiment_changes table.
 * Used in ReviewDetailResponse to show audit trail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SentimentChangeResponse {

    /**
     * Timestamp when sentiment was changed from sentiment_changes.changed_at
     */
    private ZonedDateTime changedAt;

    /**
     * Previous sentiment value from sentiment_changes.old_sentiment
     */
    private Sentiment oldSentiment;

    /**
     * New sentiment value from sentiment_changes.new_sentiment
     */
    private Sentiment newSentiment;

    /**
     * Reason for change from sentiment_changes.change_reason
     */
    private ChangeReason changeReason;

    /**
     * User ID who made the change from sentiment_changes.changed_by_user_id
     * Null for AI_INITIAL and AI_REANALYSIS
     */
    private Long changedByUserId;
}
