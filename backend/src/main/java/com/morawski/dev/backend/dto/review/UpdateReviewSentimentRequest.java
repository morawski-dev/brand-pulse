package com.morawski.dev.backend.dto.review;

import com.morawski.dev.backend.dto.common.Sentiment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for manual sentiment correction (US-007).
 * Updates reviews.sentiment and creates entry in sentiment_changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewSentimentRequest {

    @NotNull(message = "Sentiment is required")
    private Sentiment sentiment;
}
