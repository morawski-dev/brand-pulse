package com.morawski.dev.backend.dto.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.morawski.dev.backend.dto.common.Sentiment;
import com.morawski.dev.backend.dto.common.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Detailed response DTO for review with sentiment change history.
 * Maps to reviews table + sentiment_changes table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewDetailResponse {

    /**
     * Review ID from reviews.id
     */
    private Long reviewId;

    /**
     * Review source ID from reviews.review_source_id
     */
    private Long sourceId;

    /**
     * Source platform type (joined from review_sources.source_type)
     */
    private SourceType sourceType;

    /**
     * External review ID from reviews.external_review_id
     */
    private String externalReviewId;

    /**
     * Review content from reviews.content
     */
    private String content;

    /**
     * Content hash from reviews.content_hash
     */
    private String contentHash;

    /**
     * Author name from reviews.author_name
     */
    private String authorName;

    /**
     * Star rating (1-5) from reviews.rating
     */
    private Integer rating;

    /**
     * Current sentiment from reviews.sentiment
     */
    private Sentiment sentiment;

    /**
     * Sentiment confidence score from reviews.sentiment_confidence
     */
    private BigDecimal sentimentConfidence;

    /**
     * Publication timestamp from reviews.published_at
     */
    private ZonedDateTime publishedAt;

    /**
     * Fetch timestamp from reviews.fetched_at
     */
    private ZonedDateTime fetchedAt;

    /**
     * Creation timestamp from reviews.created_at
     */
    private ZonedDateTime createdAt;

    /**
     * Last update timestamp from reviews.updated_at
     */
    private ZonedDateTime updatedAt;

    /**
     * Complete sentiment change history (from sentiment_changes table)
     * Ordered by changed_at DESC
     */
    private List<SentimentChangeResponse> sentimentChangeHistory;
}
