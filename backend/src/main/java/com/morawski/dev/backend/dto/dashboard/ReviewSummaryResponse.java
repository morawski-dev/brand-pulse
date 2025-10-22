package com.morawski.dev.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Summary DTO for review (subset of ReviewResponse).
 * Used in dashboard to show recent negative reviews.
 * Maps to reviews table (selected fields).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {

    /**
     * Review ID from reviews.id
     */
    private Long reviewId;

    /**
     * Star rating from reviews.rating
     */
    private Integer rating;

    /**
     * Review content from reviews.content
     */
    private String content;

    /**
     * Publication timestamp from reviews.published_at
     */
    private ZonedDateTime publishedAt;
}
