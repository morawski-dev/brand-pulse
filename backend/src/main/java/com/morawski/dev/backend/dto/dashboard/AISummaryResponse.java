package com.morawski.dev.backend.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Response DTO for AI-generated summary.
 * Maps to ai_summaries table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AISummaryResponse {

    /**
     * Summary ID from ai_summaries.id
     */
    private Long summaryId;

    /**
     * Review source ID from ai_summaries.review_source_id (optional)
     */
    private Long sourceId;

    /**
     * Generated summary text from ai_summaries.summary_text
     */
    private String text;

    /**
     * AI model used from ai_summaries.model_used
     */
    private String modelUsed;

    /**
     * Token count from ai_summaries.token_count
     */
    private Integer tokenCount;

    /**
     * Generation timestamp from ai_summaries.generated_at
     */
    private ZonedDateTime generatedAt;

    /**
     * Validity expiration timestamp from ai_summaries.valid_until
     */
    private ZonedDateTime validUntil;
}
